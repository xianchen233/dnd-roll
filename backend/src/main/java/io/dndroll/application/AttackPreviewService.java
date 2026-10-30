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
    public record AmmunitionReview(long expectedVersion,String previewHash,String resourceId,boolean reviewed) {}
    public record AmmunitionConfirm(UUID requestId,long expectedVersion,String previewHash,String resourceId,boolean reviewed,String reason) {
        AmmunitionReview review(){return new AmmunitionReview(expectedVersion,previewHash,resourceId,reviewed);}
    }
    public record MonsterDeathReview(long expectedVersion,String previewHash,String treatment,boolean reviewed) {}
    public record MonsterDeathConfirm(UUID requestId,long expectedVersion,String previewHash,String treatment,boolean reviewed,String reason) {
        MonsterDeathReview review(){return new MonsterDeathReview(expectedVersion,previewHash,treatment,reviewed);}
    }
    public record KnockoutReview(long expectedVersion,String previewHash,String treatment,boolean reviewed) {}
    public record KnockoutConfirm(UUID requestId,long expectedVersion,String previewHash,String treatment,boolean reviewed,String reason) {
        KnockoutReview review(){return new KnockoutReview(expectedVersion,previewHash,treatment,reviewed);}
    }
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
    private DamageEffect.Plan damageEffect(Preview p){return DamageEffect.plan(p.evidence().target().sheet(),p.evidence().resolution().damage());}
    public DamageEffect.Plan damageEffect(UUID c,UUID id,String actor){return cards.locked(c,()->{
        dm(c,actor);var p=get(c,id);conflict(Set.of("PREVIEW","CONFIRMED").contains(p.status()));originals(c,p,actor);return damageEffect(p);
    });}
    private static void ammunitionInput(AmmunitionReview input){
        if(input.expectedVersion()<0)throw CharacterSheet.invalid();CharacterSheet.text(input.previewHash(),64,true);CharacterSheet.text(input.resourceId(),100,true);
        if(!input.previewHash().matches("[a-f0-9]{64}"))throw CharacterSheet.invalid();
    }
    private AmmunitionEffect.Plan ammunitionPlan(Preview p,AmmunitionReview input){
        var e=p.evidence();if(e.source().id().equals(e.target().id()))return AmmunitionEffect.manual(input.resourceId(),List.of("DISTINCT_TARGET_REQUIRED"));
        var attack=e.source().sheet().attacks().stream().filter(a->a.id().equals(e.review().attackId())).findFirst().orElseThrow(CharacterSheet::invalid);
        boolean shortbow=e.entry()!=null&&e.entry().id().equals(AmmunitionEffect.SHORTBOW)&&e.entry().equals(trusted(e.source(),e.review().attackId()))&&attack.ability().equals("DEX");
        return AmmunitionEffect.plan(e.source().sheet(),e.target().sheet(),shortbow,e.resolution().damage(),input.resourceId(),input.reviewed());
    }
    private void ammunitionOriginals(UUID c,Preview p,String actor,AmmunitionReview input){
        AccountPolicy.version(p.version(),input.expectedVersion());conflict(p.hash().equals(input.previewHash()));conflict(p.status().equals("PREVIEW"));originals(c,p,actor);
    }
    public AmmunitionEffect.Plan ammunitionEffect(UUID c,UUID id,String actor,AmmunitionReview input){
        ammunitionInput(input);return cards.locked(c,()->{dm(c,actor);var p=get(c,id);ammunitionOriginals(c,p,actor,input);return ammunitionPlan(p,input);});
    }
    public CombatService.Ack confirmAmmunition(UUID c,UUID id,String actor,AmmunitionConfirm input){
        if(input.requestId()==null)throw CharacterSheet.invalid();ammunitionInput(input.review());CharacterSheet.text(input.reason(),1000,true);
        return cards.actorLocked(actor,()->cards.locked(c,()->{
            dm(c,actor);var p=get(c,id);card(c,p.evidence().source().id());card(c,p.evidence().target().id());
            var fingerprint=fp(c,id,"CONFIRM_AMMUNITION",input);var prior=replay(actor,input.requestId(),fingerprint);if(prior!=null)return prior;
            ammunitionOriginals(c,p,actor,input.review());var plan=ammunitionPlan(p,input.review());conflict(plan.status().equals("CANDIDATE"));
            var source=p.evidence().source();var target=p.evidence().target();var b=source.sheet().combat();
            var derived=List.of(new CombatService.Change(source.id(),source.version(),b.hp(),b.tempHp(),List.of(new CombatService.ResourceValue(plan.resourceId(),plan.after()))),new CombatService.Change(target.id(),target.version(),plan.hp(),plan.tempHp(),List.of()));
            changes(c,p,derived);var journal=new ArrayList<>(p.events());journal.add(new Event(UUID.randomUUID(),"CONFIRM_AMMUNITION",actor,clock.instant(),derived,"",input.reason(),"",""));
            return finish(c,actor,input.requestId(),fingerprint,new Preview(id,p.version()+1,"CONFIRMED",p.actor(),p.at(),p.evidence(),p.hash(),List.copyOf(journal)),false,true);
        }));
    }
    private static void monsterDeathInput(MonsterDeathReview i){
        if(i.expectedVersion()<0)throw CharacterSheet.invalid();CharacterSheet.text(i.previewHash(),64,true);
        if(!i.previewHash().matches("[a-f0-9]{64}")||i.treatment()==null||!Set.of("MONSTER_DIES_AT_ZERO","MANUAL").contains(i.treatment()))throw CharacterSheet.invalid();
    }
    private void monsterDeathOriginals(UUID c,Preview p,String actor,MonsterDeathReview i){
        AccountPolicy.version(p.version(),i.expectedVersion());conflict(p.hash().equals(i.previewHash()));conflict(Set.of("PREVIEW","CONFIRMED").contains(p.status()));originals(c,p,actor);
    }
    private MonsterDeathEffect.Plan monsterDeathPlan(Preview p,MonsterDeathReview i){var e=p.evidence();return MonsterDeathEffect.plan(e.target().sheet(),e.resolution().damage(),!e.source().id().equals(e.target().id()),i.treatment(),i.reviewed());}
    public MonsterDeathEffect.Plan monsterDeathEffect(UUID c,UUID id,String actor,MonsterDeathReview i){
        monsterDeathInput(i);return cards.locked(c,()->{dm(c,actor);var p=get(c,id);monsterDeathOriginals(c,p,actor,i);return monsterDeathPlan(p,i);});
    }
    public CombatService.Ack confirmMonsterDeath(UUID c,UUID id,String actor,MonsterDeathConfirm i){
        if(i.requestId()==null)throw CharacterSheet.invalid();monsterDeathInput(i.review());CharacterSheet.text(i.reason(),1000,true);
        return cards.actorLocked(actor,()->cards.locked(c,()->{
            dm(c,actor);var p=get(c,id);card(c,p.evidence().source().id());card(c,p.evidence().target().id());
            var fingerprint=fp(c,id,"CONFIRM_MONSTER_DEATH",i);var prior=replay(actor,i.requestId(),fingerprint);if(prior!=null)return prior;
            monsterDeathOriginals(c,p,actor,i.review());conflict(p.status().equals("PREVIEW"));var plan=monsterDeathPlan(p,i.review());conflict(plan.status().equals("CANDIDATE"));
            var target=p.evidence().target();var derived=List.of(new CombatService.Change(target.id(),target.version(),plan.hp(),plan.tempHp(),List.of()));changes(c,p,derived);
            var journal=new ArrayList<>(p.events());journal.add(new Event(UUID.randomUUID(),"CONFIRM_MONSTER_DEATH",actor,clock.instant(),derived,"",i.reason(),"",""));
            return finish(c,actor,i.requestId(),fingerprint,new Preview(id,p.version()+1,"CONFIRMED",p.actor(),p.at(),p.evidence(),p.hash(),List.copyOf(journal)),false,true);
        }));
    }
    private static void knockoutInput(KnockoutReview i){
        if(i.expectedVersion()<0)throw CharacterSheet.invalid();CharacterSheet.text(i.previewHash(),64,true);
        if(!i.previewHash().matches("[a-f0-9]{64}")||i.treatment()==null||!Set.of("KNOCK_OUT_MELEE","MANUAL").contains(i.treatment()))throw CharacterSheet.invalid();
    }
    private void knockoutOriginals(UUID c,Preview p,String actor,KnockoutReview i){
        AccountPolicy.version(p.version(),i.expectedVersion());conflict(p.hash().equals(i.previewHash()));conflict(Set.of("PREVIEW","CONFIRMED").contains(p.status()));originals(c,p,actor);
    }
    private KnockoutEffect.Plan knockoutPlan(Preview p,KnockoutReview i){
        var e=p.evidence();var attack=e.source().sheet().attacks().stream().filter(a->a.id().equals(e.review().attackId())).findFirst().orElseThrow(CharacterSheet::invalid);
        boolean club=e.entry()!=null&&e.entry().id().equals(KnockoutEffect.CLUB)&&e.entry().equals(trusted(e.source(),e.review().attackId()))&&attack.ability().equals("STR");
        return KnockoutEffect.plan(e.target().sheet(),e.resolution().damage(),club,!e.source().id().equals(e.target().id()),i.treatment(),i.reviewed());
    }
    public KnockoutEffect.Plan knockoutEffect(UUID c,UUID id,String actor,KnockoutReview i){
        knockoutInput(i);return cards.locked(c,()->{dm(c,actor);var p=get(c,id);knockoutOriginals(c,p,actor,i);return knockoutPlan(p,i);});
    }
    public CombatService.Ack confirmKnockout(UUID c,UUID id,String actor,KnockoutConfirm i){
        if(i.requestId()==null)throw CharacterSheet.invalid();knockoutInput(i.review());CharacterSheet.text(i.reason(),1000,true);
        return cards.actorLocked(actor,()->cards.locked(c,()->{
            dm(c,actor);var p=get(c,id);card(c,p.evidence().source().id());card(c,p.evidence().target().id());
            var fingerprint=fp(c,id,"CONFIRM_KNOCKOUT",i);var prior=replay(actor,i.requestId(),fingerprint);if(prior!=null)return prior;
            knockoutOriginals(c,p,actor,i.review());conflict(p.status().equals("PREVIEW"));var plan=knockoutPlan(p,i.review());conflict(plan.status().equals("CANDIDATE"));
            var target=p.evidence().target();var derived=List.of(new CombatService.Change(target.id(),target.version(),plan.hp(),plan.tempHp(),List.of()));changes(c,p,derived);
            var journal=new ArrayList<>(p.events());journal.add(new Event(UUID.randomUUID(),"CONFIRM_KNOCKOUT",actor,clock.instant(),derived,"",i.reason(),"",""));
            return finish(c,actor,i.requestId(),fingerprint,new Preview(id,p.version()+1,"CONFIRMED",p.actor(),p.at(),p.evidence(),p.hash(),List.copyOf(journal)),false,true);
        }));
    }
    private static void daggerKnockoutInput(KnockoutReview i){
        if(i.expectedVersion()<0)throw CharacterSheet.invalid();CharacterSheet.text(i.previewHash(),64,true);
        if(!i.previewHash().matches("[a-f0-9]{64}")||i.treatment()==null||!Set.of("KNOCK_OUT_MELEE","THROWN","MANUAL").contains(i.treatment()))throw CharacterSheet.invalid();
    }
    private void daggerKnockoutOriginals(UUID c,Preview p,String actor,KnockoutReview i){
        AccountPolicy.version(p.version(),i.expectedVersion());conflict(p.hash().equals(i.previewHash()));conflict(Set.of("PREVIEW","CONFIRMED").contains(p.status()));originals(c,p,actor);
    }
    private KnockoutEffect.Plan daggerKnockoutPlan(Preview p,KnockoutReview i){
        var e=p.evidence();var attack=e.source().sheet().attacks().stream().filter(a->a.id().equals(e.review().attackId())).findFirst().orElseThrow(CharacterSheet::invalid);
        boolean dagger=e.entry()!=null&&e.entry().id().equals(DaggerKnockoutEffect.DAGGER)&&e.entry().equals(trusted(e.source(),e.review().attackId()))&&Set.of("STR","DEX").contains(attack.ability());
        return DaggerKnockoutEffect.plan(e.target().sheet(),e.resolution().damage(),dagger,!e.source().id().equals(e.target().id()),i.treatment(),i.reviewed());
    }
    public KnockoutEffect.Plan daggerKnockoutEffect(UUID c,UUID id,String actor,KnockoutReview i){
        daggerKnockoutInput(i);return cards.locked(c,()->{dm(c,actor);var p=get(c,id);daggerKnockoutOriginals(c,p,actor,i);return daggerKnockoutPlan(p,i);});
    }
    public CombatService.Ack confirmDaggerKnockout(UUID c,UUID id,String actor,KnockoutConfirm i){
        if(i.requestId()==null)throw CharacterSheet.invalid();daggerKnockoutInput(i.review());CharacterSheet.text(i.reason(),1000,true);
        return cards.actorLocked(actor,()->cards.locked(c,()->{
            dm(c,actor);var p=get(c,id);card(c,p.evidence().source().id());card(c,p.evidence().target().id());
            var fingerprint=fp(c,id,"CONFIRM_DAGGER_KNOCKOUT",i);var prior=replay(actor,i.requestId(),fingerprint);if(prior!=null)return prior;
            daggerKnockoutOriginals(c,p,actor,i.review());conflict(p.status().equals("PREVIEW"));var plan=daggerKnockoutPlan(p,i.review());conflict(plan.status().equals("CANDIDATE"));
            var target=p.evidence().target();var derived=List.of(new CombatService.Change(target.id(),target.version(),plan.hp(),plan.tempHp(),List.of()));changes(c,p,derived);
            var journal=new ArrayList<>(p.events());journal.add(new Event(UUID.randomUUID(),"CONFIRM_DAGGER_KNOCKOUT",actor,clock.instant(),derived,"",i.reason(),"",""));
            return finish(c,actor,i.requestId(),fingerprint,new Preview(id,p.version()+1,"CONFIRMED",p.actor(),p.at(),p.evidence(),p.hash(),List.copyOf(journal)),false,true);
        }));
    }
    public record HealingReview(long expectedVersion,String previewHash,long targetVersion,int amount,boolean reviewed) {}
    public record HealingApply(UUID requestId,long expectedVersion,String previewHash,long targetVersion,int amount,boolean reviewed,String planHash,String reason) {
        HealingReview review(){return new HealingReview(expectedVersion,previewHash,targetVersion,amount,reviewed);}
    }
    public record HealingPlan(String profile,String status,List<String> reasons,Integer hp,Integer tempHp,Integer healed,String conditionId,long targetVersion,String hash) {}
    private static void healingInput(HealingReview i){
        if(i.expectedVersion()<0||i.targetVersion()<0||i.amount()<1||i.amount()>100000)throw CharacterSheet.invalid();
        CharacterSheet.text(i.previewHash(),64,true);if(!i.previewHash().matches("[a-f0-9]{64}"))throw CharacterSheet.invalid();
    }
    private record KnockoutOrigin(CharacterSheet.Condition annotation,boolean originalVersion) {}
    private KnockoutOrigin knockoutOrigin(Preview p,CharacterService.Card target){
        var confirmations=p.events().stream().filter(e->Set.of("CONFIRM_KNOCKOUT","CONFIRM_DAGGER_KNOCKOUT").contains(e.kind())).toList();
        CharacterSheet.Condition annotation=null;boolean original=false;
        if(confirmations.size()==1){
            var confirmed=confirmations.getFirst();annotation=confirmed.kind().equals("CONFIRM_KNOCKOUT")?KnockoutEffect.annotation(p.id()):DaggerKnockoutEffect.annotation(p.id());
            original=confirmed.changes().size()==1&&confirmed.changes().getFirst().characterId().equals(target.id())&&confirmed.changes().getFirst().expectedVersion()+1==target.version()
                &&p.events().stream().anyMatch(e->e.kind().equals("APPLY"))&&p.events().stream().noneMatch(e->Set.of("CORRECT","HEAL_KNOCKOUT","END_KNOCKOUT_REST","END_KNOCKOUT_FIRST_AID").contains(e.kind()));
        }
        return new KnockoutOrigin(annotation,original);
    }
    private HealingPlan healingPlan(Preview p,CharacterService.Card target,HealingReview i){
        AccountPolicy.version(p.version(),i.expectedVersion());AccountPolicy.version(target.version(),i.targetVersion());
        conflict(p.hash().equals(i.previewHash()));conflict(p.status().equals("APPLIED"));var origin=knockoutOrigin(p,target);
        var plan=KnockoutHealingEffect.plan(target.sheet(),origin.annotation(),origin.originalVersion(),i.amount(),i.reviewed());
        return new HealingPlan(plan.profile(),plan.status(),plan.reasons(),plan.hp(),plan.tempHp(),plan.healed(),plan.conditionId(),target.version(),fp(p.id(),p.version(),p.hash(),target,i,plan));
    }
    public HealingPlan knockoutHealingEffect(UUID c,UUID id,String actor,HealingReview i){
        healingInput(i);return cards.locked(c,()->{dm(c,actor);var p=get(c,id);card(c,p.evidence().source().id());return healingPlan(p,card(c,p.evidence().target().id()),i);});
    }
    public CombatService.Ack healKnockout(UUID c,UUID id,String actor,HealingApply i){
        if(i.requestId()==null)throw CharacterSheet.invalid();healingInput(i.review());CharacterSheet.text(i.reason(),1000,true);CharacterSheet.text(i.planHash(),64,true);
        if(!i.planHash().matches("[a-f0-9]{64}"))throw CharacterSheet.invalid();
        return cards.actorLocked(actor,()->cards.locked(c,()->{
            dm(c,actor);var p=get(c,id);card(c,p.evidence().source().id());var before=card(c,p.evidence().target().id());
            var fingerprint=fp(c,id,"HEAL_KNOCKOUT",i);var prior=replay(actor,i.requestId(),fingerprint);if(prior!=null)return prior;
            var plan=healingPlan(p,before,i.review());conflict(plan.status().equals("CANDIDATE")&&plan.hash().equals(i.planHash()));
            var s=before.sheet();var b=s.combat();var sheet=s.change(s.name(),new CharacterSheet.Combat(b.maxHp(),plan.hp(),plan.tempHp(),b.speed(),b.deathSuccesses(),b.deathFailures(),b.acMode(),b.manualAc(),b.source()),s.resources(),s.conditions().stream().filter(v->!v.id().equals(plan.conditionId())).toList(),s.overrides());sheet.validate();
            var after=new CharacterService.Card(before.id(),c,before.version()+1,before.hidden(),before.shareFull(),before.importReview(),before.controllers(),sheet);
            cards.save(after,false);cards.history(new CharacterService.History(after.version(),actor,"KNOCKOUT_HEALING",i.reason(),clock.instant(),before,after));
            var journal=new ArrayList<>(p.events());journal.add(new Event(UUID.randomUUID(),"HEAL_KNOCKOUT",actor,clock.instant(),List.of(new CombatService.Change(before.id(),before.version(),plan.hp(),plan.tempHp(),List.of())),"",i.reason(),"",""));
            return finish(c,actor,i.requestId(),fingerprint,new Preview(id,p.version()+1,p.status(),p.actor(),p.at(),p.evidence(),p.hash(),List.copyOf(journal)),false,false);
        }));
    }
    public record RestReview(long expectedVersion,String previewHash,long targetVersion,int minutes,boolean interrupted,boolean reviewed) {}
    public record RestComplete(UUID requestId,long expectedVersion,String previewHash,long targetVersion,int minutes,boolean interrupted,boolean reviewed,String planHash,String reason) {
        RestReview review(){return new RestReview(expectedVersion,previewHash,targetVersion,minutes,interrupted,reviewed);}
    }
    public record RestPlan(String profile,String status,List<String> reasons,Integer hp,Integer tempHp,String conditionId,long targetVersion,String hash) {}
    private static void restInput(RestReview i){
        if(i.expectedVersion()<0||i.targetVersion()<0||i.minutes()<0||i.minutes()>100000)throw CharacterSheet.invalid();
        CharacterSheet.text(i.previewHash(),64,true);if(!i.previewHash().matches("[a-f0-9]{64}"))throw CharacterSheet.invalid();
    }
    private RestPlan restPlan(Preview p,CharacterService.Card target,RestReview i){
        AccountPolicy.version(p.version(),i.expectedVersion());AccountPolicy.version(target.version(),i.targetVersion());
        conflict(p.hash().equals(i.previewHash()));conflict(p.status().equals("APPLIED"));var origin=knockoutOrigin(p,target);
        var plan=KnockoutRestEffect.plan(target.sheet(),origin.annotation(),origin.originalVersion(),i.minutes(),i.interrupted(),i.reviewed());
        return new RestPlan(plan.profile(),plan.status(),plan.reasons(),plan.hp(),plan.tempHp(),plan.conditionId(),target.version(),fp(p.id(),p.version(),p.hash(),target,i,plan));
    }
    public RestPlan knockoutRestEffect(UUID c,UUID id,String actor,RestReview i){
        restInput(i);return cards.locked(c,()->{dm(c,actor);var p=get(c,id);card(c,p.evidence().source().id());return restPlan(p,card(c,p.evidence().target().id()),i);});
    }
    public CombatService.Ack completeKnockoutRest(UUID c,UUID id,String actor,RestComplete i){
        if(i.requestId()==null)throw CharacterSheet.invalid();restInput(i.review());CharacterSheet.text(i.reason(),1000,true);CharacterSheet.text(i.planHash(),64,true);
        if(!i.planHash().matches("[a-f0-9]{64}"))throw CharacterSheet.invalid();
        return cards.actorLocked(actor,()->cards.locked(c,()->{
            dm(c,actor);var p=get(c,id);card(c,p.evidence().source().id());var before=card(c,p.evidence().target().id());
            var fingerprint=fp(c,id,"END_KNOCKOUT_REST",i);var prior=replay(actor,i.requestId(),fingerprint);if(prior!=null)return prior;
            var plan=restPlan(p,before,i.review());conflict(plan.status().equals("CANDIDATE")&&plan.hash().equals(i.planHash()));
            var s=before.sheet();var sheet=s.change(s.name(),s.combat(),s.resources(),s.conditions().stream().filter(v->!v.id().equals(plan.conditionId())).toList(),s.overrides());sheet.validate();
            var after=new CharacterService.Card(before.id(),c,before.version()+1,before.hidden(),before.shareFull(),before.importReview(),before.controllers(),sheet);
            cards.save(after,false);cards.history(new CharacterService.History(after.version(),actor,"KNOCKOUT_REST",i.reason(),clock.instant(),before,after));
            var restSummary="主持人已确认：从此次击昏开始连续短休"+i.minutes()+"分钟，未中断且活动合规；醒来仍倒地。";
            var journal=new ArrayList<>(p.events());journal.add(new Event(UUID.randomUUID(),"END_KNOCKOUT_REST",actor,clock.instant(),List.of(new CombatService.Change(before.id(),before.version(),plan.hp(),plan.tempHp(),List.of())),"",i.reason(),restSummary,""));
            return finish(c,actor,i.requestId(),fingerprint,new Preview(id,p.version()+1,p.status(),p.actor(),p.at(),p.evidence(),p.hash(),List.copyOf(journal)),false,false);
        }));
    }
    public record FirstAidAttempt(UUID requestId,long expectedVersion,String previewHash,long targetVersion,UUID helperId,long helperVersion,boolean reviewed,String reason) {}
    public record FirstAidReview(long expectedVersion,String previewHash,long targetVersion,UUID helperId,long helperVersion,UUID rollId,boolean reviewed) {}
    public record FirstAidComplete(UUID requestId,long expectedVersion,String previewHash,long targetVersion,UUID helperId,long helperVersion,UUID rollId,boolean reviewed,String planHash,String reason){
        FirstAidReview review(){return new FirstAidReview(expectedVersion,previewHash,targetVersion,helperId,helperVersion,rollId,reviewed);}
    }
    public record FirstAidPlan(String profile,String status,List<String> reasons,int dc,Integer total,Integer hp,Integer tempHp,String conditionId,long targetVersion,String hash) {}
    private static void firstAidInput(long version,String hash,long targetVersion,UUID helper,long helperVersion){
        if(version<0||targetVersion<0||helperVersion<0||helper==null)throw CharacterSheet.invalid();CharacterSheet.text(hash,64,true);if(!hash.matches("[a-f0-9]{64}"))throw CharacterSheet.invalid();
    }
    private void firstAidVersions(Preview p,CharacterService.Card target,CharacterService.Card helper,long version,String hash,long targetVersion,long helperVersion){
        AccountPolicy.version(p.version(),version);AccountPolicy.version(target.version(),targetVersion);AccountPolicy.version(helper.version(),helperVersion);conflict(p.hash().equals(hash)&&p.status().equals("APPLIED"));
    }
    private boolean aidHelper(CharacterService.Card helper){return !helper.importReview()&&KnockoutFirstAidEffect.helperSupported(helper.sheet());}
    public CombatService.Ack rollKnockoutFirstAid(UUID c,UUID id,String actor,FirstAidAttempt i){
        if(i.requestId()==null)throw CharacterSheet.invalid();firstAidInput(i.expectedVersion(),i.previewHash(),i.targetVersion(),i.helperId(),i.helperVersion());CharacterSheet.text(i.reason(),1000,true);
        return cards.actorLocked(actor,()->cards.locked(c,()->{
            dm(c,actor);var p=get(c,id);card(c,p.evidence().source().id());var target=card(c,p.evidence().target().id());var helper=card(c,i.helperId());
            var fingerprint=fp(c,id,"ROLL_KNOCKOUT_FIRST_AID",i);var prior=replay(actor,i.requestId(),fingerprint);if(prior!=null)return prior;
            firstAidVersions(p,target,helper,i.expectedVersion(),i.previewHash(),i.targetVersion(),i.helperVersion());var origin=knockoutOrigin(p,target);
            conflict(KnockoutFirstAidEffect.eligibility(target.sheet(),origin.annotation(),origin.originalVersion(),aidHelper(helper),!target.id().equals(helper.id()),i.reviewed()).isEmpty());
            var roll=dice.plannedFirstAid(c,actor,child(i.requestId(),"first-aid"),helper,p.id(),p.version());var changes=List.of(new CombatService.Change(helper.id(),helper.version(),helper.sheet().combat().hp(),helper.sheet().combat().tempHp(),List.of()),new CombatService.Change(target.id(),target.version(),target.sheet().combat().hp(),target.sheet().combat().tempHp(),List.of()));
            var summary="急救动作检定："+helper.sheet().name()+"，感知（医药）总值"+roll.result().total()+" / DC10；"+(roll.result().total()>=10?"成功，仍需另行确认解除。":"失败，未解除。")+"检定ID："+roll.id();
            var journal=new ArrayList<>(p.events());journal.add(new Event(roll.id(),"ROLL_KNOCKOUT_FIRST_AID",actor,clock.instant(),changes,"",i.reason(),summary,""));
            return finish(c,actor,i.requestId(),fingerprint,new Preview(id,p.version()+1,p.status(),p.actor(),p.at(),p.evidence(),p.hash(),List.copyOf(journal)),false,true);
        }));
    }
    private FirstAidPlan firstAidPlan(UUID c,String actor,Preview p,CharacterService.Card target,CharacterService.Card helper,FirstAidReview i){
        firstAidVersions(p,target,helper,i.expectedVersion(),i.previewHash(),i.targetVersion(),i.helperVersion());var origin=knockoutOrigin(p,target);var roll=dice.detail(c,i.rollId(),actor);
        var last=p.events().stream().filter(e->e.kind().equals("ROLL_KNOCKOUT_FIRST_AID")).reduce((a,b)->b).orElse(null);
        boolean bound=last!=null&&last.id().equals(roll.id())&&last.actor().equals(roll.actor())&&last.changes().size()==2
            &&last.changes().get(0).characterId().equals(helper.id())&&last.changes().get(0).expectedVersion()==helper.version()
            &&last.changes().get(1).characterId().equals(target.id())&&last.changes().get(1).expectedVersion()==target.version()
            &&roll.characterId().equals(helper.id().toString())&&roll.sourceVersion()==helper.version()&&roll.visibility().equals("DM")&&roll.key().equals("skill:medicine")
            &&roll.calculation().equals(helper.sheet().calculate().get("skill:medicine"))&&roll.advantage().isEmpty()&&roll.disadvantage().isEmpty();
        var plan=KnockoutFirstAidEffect.plan(target.sheet(),origin.annotation(),origin.originalVersion(),aidHelper(helper),!target.id().equals(helper.id()),i.reviewed(),bound,roll.result().total());
        return new FirstAidPlan(plan.profile(),plan.status(),plan.reasons(),plan.dc(),plan.total(),plan.hp(),plan.tempHp(),plan.conditionId(),target.version(),fp(p.id(),p.version(),p.hash(),target,helper,i,roll,plan));
    }
    public FirstAidPlan knockoutFirstAidEffect(UUID c,UUID id,String actor,FirstAidReview i){
        firstAidInput(i.expectedVersion(),i.previewHash(),i.targetVersion(),i.helperId(),i.helperVersion());if(i.rollId()==null)throw CharacterSheet.invalid();
        return cards.locked(c,()->{dm(c,actor);var p=get(c,id);card(c,p.evidence().source().id());return firstAidPlan(c,actor,p,card(c,p.evidence().target().id()),card(c,i.helperId()),i);});
    }
    public CombatService.Ack completeKnockoutFirstAid(UUID c,UUID id,String actor,FirstAidComplete i){
        if(i.requestId()==null||i.rollId()==null)throw CharacterSheet.invalid();firstAidInput(i.expectedVersion(),i.previewHash(),i.targetVersion(),i.helperId(),i.helperVersion());CharacterSheet.text(i.reason(),1000,true);CharacterSheet.text(i.planHash(),64,true);if(!i.planHash().matches("[a-f0-9]{64}"))throw CharacterSheet.invalid();
        return cards.actorLocked(actor,()->cards.locked(c,()->{
            dm(c,actor);var p=get(c,id);card(c,p.evidence().source().id());var before=card(c,p.evidence().target().id());var helper=card(c,i.helperId());var fingerprint=fp(c,id,"END_KNOCKOUT_FIRST_AID",i);var prior=replay(actor,i.requestId(),fingerprint);if(prior!=null)return prior;
            var plan=firstAidPlan(c,actor,p,before,helper,i.review());conflict(plan.status().equals("CANDIDATE")&&plan.hash().equals(i.planHash()));
            var s=before.sheet();var sheet=s.change(s.name(),s.combat(),s.resources(),s.conditions().stream().filter(v->!v.id().equals(plan.conditionId())).toList(),s.overrides());sheet.validate();
            var after=new CharacterService.Card(before.id(),c,before.version()+1,before.hidden(),before.shareFull(),before.importReview(),before.controllers(),sheet);cards.save(after,false);cards.history(new CharacterService.History(after.version(),actor,"KNOCKOUT_FIRST_AID",i.reason(),clock.instant(),before,after));
            var summary="急救成功已确认：检定ID "+i.rollId()+"，感知（医药）总值"+plan.total()+" / DC10；解除原击昏记录，醒来仍倒地。";
            var journal=new ArrayList<>(p.events());journal.add(new Event(UUID.randomUUID(),"END_KNOCKOUT_FIRST_AID",actor,clock.instant(),List.of(new CombatService.Change(before.id(),before.version(),plan.hp(),plan.tempHp(),List.of())),"",i.reason(),summary,""));
            return finish(c,actor,i.requestId(),fingerprint,new Preview(id,p.version()+1,p.status(),p.actor(),p.at(),p.evidence(),p.hash(),List.copyOf(journal)),false,false);
        }));
    }
    public CombatService.Ack command(UUID c,UUID id,String actor,Command input){
        if(input.requestId()==null||input.expectedVersion()<0||input.changes()==null||!Set.of("CONFIRM","CONFIRM_DAMAGE","APPLY","CORRECT","PUBLISH").contains(input.kind()))throw CharacterSheet.invalid();
        CharacterSheet.text(input.previewHash(),64,true);CharacterSheet.text(input.reason(),1000,true);CharacterSheet.text(input.correctionOf(),36,false);CharacterSheet.text(input.summary(),1000,false);
        return cards.actorLocked(actor,()->cards.locked(c,()->{
            dm(c,actor);var p=get(c,id);card(c,p.evidence().source().id());card(c,p.evidence().target().id());var fingerprint=fp(c,id,input);var prior=replay(actor,input.requestId(),fingerprint);if(prior!=null)return prior;
            AccountPolicy.version(p.version(),input.expectedVersion());conflict(p.hash().equals(input.previewHash()));
            if(!input.kind().equals("CORRECT")&&!input.correctionOf().isEmpty()||!input.kind().equals("PUBLISH")&&!input.summary().isEmpty()||Set.of("CONFIRM_DAMAGE","APPLY","PUBLISH").contains(input.kind())&&!input.changes().isEmpty())throw CharacterSheet.invalid();
            var journal=new ArrayList<>(p.events());String status=p.status(),combatId="";var eventChanges=input.changes();
            switch(input.kind()){
                case "CONFIRM"->{conflict(status.equals("PREVIEW"));originals(c,p,actor);changes(c,p,input.changes());status="CONFIRMED";}
                case "CONFIRM_DAMAGE"->{
                    conflict(status.equals("PREVIEW"));originals(c,p,actor);var plan=damageEffect(p);conflict(plan.status().equals("CANDIDATE"));
                    var target=p.evidence().target();eventChanges=List.of(new CombatService.Change(target.id(),target.version(),plan.hp(),plan.tempHp(),List.of()));
                    changes(c,p,eventChanges);status="CONFIRMED";
                }
                case "APPLY"->{
                    conflict(status.equals("CONFIRMED"));originals(c,p,actor);var confirmed=journal.stream().filter(e->Set.of("CONFIRM","CONFIRM_DAMAGE","CONFIRM_AMMUNITION","CONFIRM_MONSTER_DEATH","CONFIRM_KNOCKOUT","CONFIRM_DAGGER_KNOCKOUT").contains(e.kind())).findFirst().orElseThrow();changes(c,p,confirmed.changes());
                    var action=(CombatService.Action)combat.detail(c,p.evidence().actionId(),actor);long v=action.version();
                    var verdict=p.evidence().resolution().review().candidate().verdict();
                    combat.append(c,action.id(),actor,new CombatService.Command(child(input.requestId(),"verdict"),v++,"VERDICT","",verdict.equals("CRITICAL")?"HIT":verdict,List.of(),"",input.reason()));
                    var application=new CombatService.Command(child(input.requestId(),"apply"),v,"APPLY","","",confirmed.changes(),"",input.reason());
                    if(confirmed.kind().equals("CONFIRM_MONSTER_DEATH"))combat.appendMonsterDeath(c,action.id(),actor,application,p.id());
                    else if(confirmed.kind().equals("CONFIRM_KNOCKOUT"))combat.appendKnockout(c,action.id(),actor,application,p.id());
                    else if(confirmed.kind().equals("CONFIRM_DAGGER_KNOCKOUT"))combat.appendDaggerKnockout(c,action.id(),actor,application,p.id());
                    else combat.append(c,action.id(),actor,application);
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
            journal.add(new Event(UUID.randomUUID(),input.kind(),actor,clock.instant(),List.copyOf(eventChanges),input.correctionOf(),input.reason(),input.summary(),combatId));
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
