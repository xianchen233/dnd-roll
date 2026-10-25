package io.dndroll.application;

import io.dndroll.domain.*;
import java.time.*;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** Authenticated durable adapter around the pure calculator; all evidence is DM-only. */
public final class AttackPreviewService {
    public record Create(UUID requestId,UUID characterId,long sourceVersion,UUID targetId,long targetVersion,String attackId,
        boolean reviewed,List<AttackCalculator.Factor> exceptions,List<AttackCalculator.Factor> advantage,List<AttackCalculator.Factor> disadvantage,
        List<AttackCalculator.Factor> immunities,List<AttackCalculator.Factor> resistances,List<AttackCalculator.Factor> vulnerabilities) {
        public Create { exceptions=copyFactors(exceptions);advantage=copyFactors(advantage);disadvantage=copyFactors(disadvantage);immunities=copyFactors(immunities);resistances=copyFactors(resistances);vulnerabilities=copyFactors(vulnerabilities); }
    }
    public record Command(UUID requestId,long expectedVersion,String kind,String previewHash,List<CombatService.Change> changes,String correctionOf,String reason,String summary) {
        public Command { if(changes==null||changes.size()>2||changes.stream().anyMatch(v->v==null||v.resources()==null||v.resources().stream().anyMatch(Objects::isNull)))throw CharacterSheet.invalid();changes=changes.stream().map(v->new CombatService.Change(v.characterId(),v.expectedVersion(),v.hp(),v.tempHp(),List.copyOf(v.resources()))).toList(); }
    }
    public record Evidence(Create review,CharacterService.Card source,CharacterService.Card target,ContentEntry entry,
        List<String> manualReasons,DiceService.Roll attackRoll,DiceService.Roll damageRoll,AttackCalculator.Resolution resolution,UUID actionId) {}
    public record Event(UUID id,String kind,String actor,Instant at,List<CombatService.Change> changes,String correctionOf,String reason,String summary,String combatEventId) {}
    public record Preview(UUID id,long version,String status,String actor,Instant at,Evidence evidence,String hash,List<Event> events) {}
    public record PublicResult(UUID id,String name,String targetName,String summary,String actor,Instant at) {}
    public record Receipt(String fingerprint,CombatService.Ack result) {}
    public interface Store {
        Preview get(UUID c,UUID id);void save(UUID c,Preview p,boolean create);List<Preview> list(UUID c,boolean published);
        Receipt receipt(String actor,UUID request);void receipt(UUID c,String actor,UUID request,Receipt receipt);
    }
    private final Store store;private final CharacterService.Store cards;private final DiceService dice;private final CombatService combat;
    private final List<ContentEntry> catalog;private final DiceService.Events events;private final Clock clock;private final JsonMapper json;
    public AttackPreviewService(Store store,CharacterService.Store cards,DiceService dice,CombatService combat,List<ContentEntry> catalog,DiceService.Events events,Clock clock,JsonMapper json){
        this.store=store;this.cards=cards;this.dice=dice;this.combat=combat;this.catalog=List.copyOf(catalog);this.events=events;this.clock=clock;this.json=json;
    }
    private void dm(UUID c,String actor){if(cards.role(c,actor)!=io.dndroll.domain.Movement.Role.DM)throw new DomainException("FORBIDDEN",403,"Current campaign DM required");}
    private CharacterService.Card card(UUID c,UUID id){var v=cards.card(c,id);if(v==null)throw DiceService.missing();return v;}
    private Preview get(UUID c,UUID id){var p=store.get(c,id);if(p==null)throw DiceService.missing();return p;}
    private CombatService.Ack replay(String actor,UUID request,String fp){var r=store.receipt(actor,request);if(r==null)return null;if(!r.fingerprint().equals(fp))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID already used");return r.result();}
    private String fp(Object... v){return DiceService.fingerprint(json,Arrays.asList(v));}
    private static void conflict(boolean ok){if(!ok)throw new DomainException("VERSION_CONFLICT",409,"Preview changed or command is not available");}
    private static void factors(List<AttackCalculator.Factor> values){if(values==null||values.size()>10||values.stream().anyMatch(Objects::isNull))throw CharacterSheet.invalid();}
    private static List<AttackCalculator.Factor> copyFactors(List<AttackCalculator.Factor> values){factors(values);return List.copyOf(values);}
    private static List<String> diceLabels(List<AttackCalculator.Factor> values){return values.stream().map(v->{String r=v.reason();return r.substring(0,r.offsetByCodePoints(0,Math.min(200,r.codePointCount(0,r.length()))));}).toList();}
    private static UUID child(UUID request,String purpose){return UUID.nameUUIDFromBytes(("A05:"+request+":"+purpose).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private CombatService.Ack finish(UUID c,String actor,UUID request,String fp,Preview p,boolean create,boolean privateOnly){
        store.save(c,p,create);var ack=new CombatService.Ack(p.id(),p.version());store.receipt(c,actor,request,new Receipt(fp,ack));events.changed(c,privateOnly);return ack;
    }
    private ContentEntry trusted(CharacterService.Card source,String attackId){
        var a=source.sheet().attacks().stream().filter(x->x.id().equals(attackId)).findFirst().orElseThrow(CharacterSheet::invalid);
        return catalog.stream().filter(e->e.builtin()&&e.data().type().equals("ATTACK")&&attackId.equals("content:"+e.id())).filter(e->{
            var canonical=e.fill(CharacterSheet.empty("Identity check"),a.ability(),"").attacks().getFirst();
            return a.name().equals(canonical.name())&&a.source().equals(canonical.source())&&a.dice().equals(canonical.dice())&&a.damageType().equals(canonical.damageType())&&a.mastery().isEmpty();
        }).findFirst().orElse(null);
    }
    private AttackCalculator.Request request(Create i,CharacterService.Card source,CharacterService.Card target,ContentEntry entry,List<Integer> d20){
        var a=source.sheet().attacks().stream().filter(x->x.id().equals(i.attackId())).findFirst().orElseThrow();
        var m=java.util.regex.Pattern.compile("([1-9][0-9]?)d([1-9][0-9]{0,3})").matcher(entry.data().dice());if(!m.matches())throw CharacterSheet.invalid();
        var type=switch(entry.data().damageType()){case "穿刺"->AttackCalculator.DamageType.PIERCING;case "挥砍"->AttackCalculator.DamageType.SLASHING;case "钝击"->AttackCalculator.DamageType.BLUDGEONING;default->throw CharacterSheet.invalid();};
        var calc=source.sheet().calculate();
        return new AttackCalculator.Request(AttackCalculator.PROFILE,"WEAPON_ATTACK",a.source(),new AttackCalculator.Context(i.reviewed(),i.exceptions()),new AttackCalculator.Roll(d20,i.advantage(),i.disadvantage()),
            calc.get("attack:"+a.id()).value(),target.sheet().calculate().get("ac").value(),new AttackCalculator.Damage(Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2)),calc.get("damage:"+a.id()).value(),type,i.immunities(),i.resistances(),i.vulnerabilities()));
    }
    public CombatService.Ack create(UUID c,String actor,Create input){
        if(input.requestId()==null||input.characterId()==null||input.targetId()==null||input.sourceVersion()<0||input.targetVersion()<0)throw CharacterSheet.invalid();CharacterSheet.text(input.attackId(),100,true);
        for(var values:Arrays.asList(input.exceptions(),input.advantage(),input.disadvantage(),input.immunities(),input.resistances(),input.vulnerabilities()))factors(values);
        return cards.actorLocked(actor,()->cards.locked(c,()->{
            dm(c,actor);var source=card(c,input.characterId());var target=card(c,input.targetId());var fingerprint=fp(c,"CREATE",input);var prior=replay(actor,input.requestId(),fingerprint);if(prior!=null)return prior;
            AccountPolicy.version(source.version(),input.sourceVersion());AccountPolicy.version(target.version(),input.targetVersion());
            var entry=trusted(source,input.attackId());var reasons=new ArrayList<String>();AttackCalculator.Resolution resolution=null;DiceService.Roll attackRoll=null,damageRoll=null;UUID actionId=null;
            if(entry==null)reasons.add("UNSUPPORTED_CATALOG_ATTACK_OR_MASTERY");
            else{
                var dummy=input.advantage().isEmpty()==input.disadvantage().isEmpty()?List.of(1):List.of(1,1);
                reasons.addAll(AttackCalculator.plan(request(input,source,target,entry,dummy)).reasons());
                if(reasons.isEmpty()){
                    attackRoll=dice.roll(c,actor,new DiceService.Command(child(input.requestId(),"attack"),source.version(),source.id().toString(),"attack:"+input.attackId(),"","DM",diceLabels(input.advantage()),diceLabels(input.disadvantage())));
                    var req=request(input,source,target,entry,attackRoll.result().dice().stream().map(Dice.Die::value).toList());var plan=AttackCalculator.plan(req).candidate();
                    if(plan.damageCount()>0)damageRoll=dice.plannedDamage(c,actor,child(input.requestId(),"damage"),source,input.attackId(),plan.damageCount(),plan.damageSides(),req.damage().modifier());
                    resolution=AttackCalculator.resolve(req,damageRoll==null?List.of():damageRoll.result().dice().stream().map(Dice.Die::value).toList());
                    var a=combat.declare(c,actor,new CombatService.Declare(child(input.requestId(),"action"),source.version(),source.id(),target.id(),input.attackId(),"DM"));actionId=a.id();long av=0;
                    combat.append(c,actionId,actor,new CombatService.Command(child(input.requestId(),"link-attack"),av++,"LINK_ROLL",attackRoll.id().toString(),"",List.of(),"",""));
                    if(damageRoll!=null)combat.append(c,actionId,actor,new CombatService.Command(child(input.requestId(),"link-damage"),av,"LINK_ROLL",damageRoll.id().toString(),"",List.of(),"",""));
                }
            }
            var evidence=new Evidence(input,source,target,entry,List.copyOf(reasons),attackRoll,damageRoll,resolution,actionId);
            var p=new Preview(UUID.randomUUID(),0,reasons.isEmpty()?"PREVIEW":"MANUAL_REQUIRED",actor,clock.instant(),evidence,fp(evidence),List.of());
            return finish(c,actor,input.requestId(),fingerprint,p,true,true);
        }));
    }
    private void originals(UUID c,Preview p,String actor){var e=p.evidence();AccountPolicy.version(card(c,e.source().id()).version(),e.source().version());AccountPolicy.version(card(c,e.target().id()).version(),e.target().version());var action=(CombatService.Action)combat.detail(c,e.actionId(),actor);AccountPolicy.version(action.version(),e.damageRoll()==null?1:2);}
    private void changes(UUID c,Preview p,List<CombatService.Change> changes){
        if(changes.isEmpty()||changes.size()>2)throw CharacterSheet.invalid();var ids=Set.copyOf(Arrays.asList(p.evidence().source().id(),p.evidence().target().id()));var seen=new HashSet<UUID>();
        for(var v:changes){if(v==null||!ids.contains(v.characterId())||!seen.add(v.characterId()))throw CharacterSheet.invalid();card(c,v.characterId());}
        combat.reviewChanges(c,changes);
    }
    public CombatService.Ack command(UUID c,UUID id,String actor,Command input){
        if(input.requestId()==null||input.expectedVersion()<0||input.changes()==null||!Set.of("CONFIRM","APPLY","CORRECT","PUBLISH").contains(input.kind()))throw CharacterSheet.invalid();
        CharacterSheet.text(input.previewHash(),64,true);CharacterSheet.text(input.reason(),1000,true);CharacterSheet.text(input.correctionOf(),36,false);CharacterSheet.text(input.summary(),1000,false);
        return cards.actorLocked(actor,()->cards.locked(c,()->{
            dm(c,actor);var p=get(c,id);card(c,p.evidence().source().id());card(c,p.evidence().target().id());var fingerprint=fp(c,id,input);var prior=replay(actor,input.requestId(),fingerprint);if(prior!=null)return prior;
            AccountPolicy.version(p.version(),input.expectedVersion());conflict(p.hash().equals(input.previewHash()));
            if(!input.kind().equals("CORRECT")&&!input.correctionOf().isEmpty()||!input.kind().equals("PUBLISH")&&!input.summary().isEmpty()||Set.of("APPLY","PUBLISH").contains(input.kind())&&!input.changes().isEmpty())throw CharacterSheet.invalid();
            var journal=new ArrayList<>(p.events());String status=p.status(),combatId="";
            switch(input.kind()){
                case "CONFIRM"->{conflict(status.equals("PREVIEW"));originals(c,p,actor);changes(c,p,input.changes());status="CONFIRMED";}
                case "APPLY"->{
                    conflict(status.equals("CONFIRMED"));originals(c,p,actor);var confirmed=journal.stream().filter(e->e.kind().equals("CONFIRM")).findFirst().orElseThrow();changes(c,p,confirmed.changes());
                    var action=(CombatService.Action)combat.detail(c,p.evidence().actionId(),actor);long v=action.version();
                    var verdict=p.evidence().resolution().review().candidate().verdict();
                    combat.append(c,action.id(),actor,new CombatService.Command(child(input.requestId(),"verdict"),v++,"VERDICT","",verdict.equals("CRITICAL")?"HIT":verdict,List.of(),"",input.reason()));
                    combat.append(c,action.id(),actor,new CombatService.Command(child(input.requestId(),"apply"),v,"APPLY","","",confirmed.changes(),"",input.reason()));
                    var applied=(CombatService.Action)combat.detail(c,action.id(),actor);combatId=applied.events().getLast().id().toString();status="APPLIED";
                }
                case "CORRECT"->{
                    conflict(status.equals("APPLIED"));var original=journal.stream().filter(e->e.id().toString().equals(input.correctionOf())&&Set.of("APPLY","CORRECT").contains(e.kind())).findFirst().orElseThrow(DiceService::missing);changes(c,p,input.changes());
                    var action=(CombatService.Action)combat.detail(c,p.evidence().actionId(),actor);
                    combat.append(c,action.id(),actor,new CombatService.Command(child(input.requestId(),"correct"),action.version(),"CORRECT","","",input.changes(),original.combatEventId(),input.reason()));
                    combatId=((CombatService.Action)combat.detail(c,action.id(),actor)).events().getLast().id().toString();
                }
                case "PUBLISH"->{conflict(status.equals("APPLIED"));CharacterSheet.text(input.summary(),1000,true);}
            }
            journal.add(new Event(UUID.randomUUID(),input.kind(),actor,clock.instant(),List.copyOf(input.changes()),input.correctionOf(),input.reason(),input.summary(),combatId));
            return finish(c,actor,input.requestId(),fingerprint,new Preview(id,p.version()+1,status,p.actor(),p.at(),p.evidence(),p.hash(),List.copyOf(journal)),false,!input.kind().equals("PUBLISH"));
        }));
    }
    public Preview detail(UUID c,UUID id,String actor){return cards.locked(c,()->{dm(c,actor);return get(c,id);});}
    public List<Preview> list(UUID c,String actor){return cards.locked(c,()->{dm(c,actor);return store.list(c,false);});}
    public List<PublicResult> results(UUID c,String actor){return cards.locked(c,()->{
        if(cards.role(c,actor)==null)throw new DomainException("FORBIDDEN",403,"Campaign membership required");var out=new ArrayList<PublicResult>();
        for(var p:store.list(c,true)){
            var s=card(c,p.evidence().source().id());var t=card(c,p.evidence().target().id());if(s.hidden()||s.importReview()||t.hidden()||t.importReview())continue;
            var last=p.events().stream().filter(e->e.kind().equals("PUBLISH")).reduce((a,b)->b).orElse(null);if(last!=null)out.add(new PublicResult(p.id(),p.evidence().source().sheet().name(),p.evidence().target().sheet().name(),last.summary(),last.actor(),last.at()));
        }return List.copyOf(out);
    });}
}
