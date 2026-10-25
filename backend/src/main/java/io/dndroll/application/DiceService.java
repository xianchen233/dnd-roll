package io.dndroll.application;

import io.dndroll.domain.*;
import io.dndroll.domain.CharacterSheet.Calculation;
import io.dndroll.domain.Movement.Role;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import tools.jackson.databind.json.JsonMapper;

/** One action path for structured buttons and parsed text commands. */
public final class DiceService {
    public record Command(UUID requestId,long expectedVersion,String characterId,String key,String expression,String visibility,List<String> advantage,List<String> disadvantage) {}
    public record TextCommand(UUID requestId,long expectedVersion,String characterId,String text) {}
    public record Roll(UUID id,String characterId,long sourceVersion,String actor,String name,Instant at,String visibility,String key,Calculation calculation,List<String> advantage,List<String> disadvantage,Dice.Result result) {}
    public record Listing(List<Roll> rolls,String next) {}
    public record Receipt(String fingerprint,UUID id) {}
    public interface Store {
        Roll roll(UUID campaign,UUID id);void save(UUID campaign,Roll roll);
        List<Roll> list(UUID campaign,boolean dm,UUID before,int limit);
        Receipt receipt(String actor,UUID request);void receipt(UUID campaign,String actor,UUID request,Receipt receipt);
    }
    @FunctionalInterface public interface Events { void changed(UUID campaign,boolean dmOnly); }
    private final Store store;private final CharacterService.Store cards;private final Dice.Entropy entropy;private final Events events;private final Clock clock;private final JsonMapper json;
    public DiceService(Store store,CharacterService.Store cards,Dice.Entropy entropy,Events events,Clock clock,JsonMapper json){this.store=store;this.cards=cards;this.entropy=entropy;this.events=events;this.clock=clock;this.json=json;}
    private Role member(UUID c,String actor){var role=cards.role(c,actor);require(role!=null);return role;}
    static void require(boolean ok){if(!ok)throw new DomainException("FORBIDDEN",403,"Dice permission required");}
    static DomainException missing(){return new DomainException("NOT_FOUND",404,"Record not found");}
    private boolean visible(UUID c,Roll r,Role role){
        if(r==null)return false;if(role==Role.DM)return true;if(!r.visibility().equals("PUBLIC"))return false;
        if(r.characterId().isEmpty())return true;var card=cards.card(c,UUID.fromString(r.characterId()));return card!=null&&!card.hidden()&&!card.importReview();
    }
    public Roll detail(UUID c,UUID id,String actor){return cards.locked(c,()->{var role=member(c,actor);var r=store.roll(c,id);if(!visible(c,r,role))throw missing();return r;});}
    public Listing list(UUID c,String actor,UUID before){return cards.locked(c,()->{var role=member(c,actor);if(before!=null&&!visible(c,store.roll(c,before),role))throw missing();var rows=store.list(c,role==Role.DM,before,51);boolean more=rows.size()>50;var page=List.copyOf(rows.subList(0,Math.min(rows.size(),50)));return new Listing(page,more?page.getLast().id().toString():"");});}
    public Roll text(UUID c,String actor,TextCommand t){
        CharacterSheet.text(t.text(),300,true);String[] bits=t.text().strip().split(" +");
        if(bits.length<2||bits.length>5||!Set.of("/roll","/check").contains(bits[0]))throw CharacterSheet.invalid();
        var flags=new HashSet<String>();for(int i=2;i<bits.length;i++)if(!Set.of("adv","dis","dm").contains(bits[i])||!flags.add(bits[i]))throw CharacterSheet.invalid();
        return roll(c,actor,new Command(t.requestId(),t.expectedVersion(),t.characterId(),bits[0].equals("/roll")?"EXPRESSION":bits[1],bits[0].equals("/roll")?bits[1]:"",flags.contains("dm")?"DM":"PUBLIC",flags.contains("adv")?List.of("手动优势"):List.of(),flags.contains("dis")?List.of("手动劣势"):List.of()));
    }
    public Roll roll(UUID c,String actor,Command input){
        if(input.requestId()==null||input.expectedVersion()<0)throw CharacterSheet.invalid();CharacterSheet.text(input.characterId(),36,false);CharacterSheet.text(input.key(),100,true);CharacterSheet.text(input.expression(),100,false);Dice.sources(input.advantage());Dice.sources(input.disadvantage());
        if(!Set.of("PUBLIC","DM").contains(input.visibility()))throw CharacterSheet.invalid();
        String normalized=input.key().equals("EXPRESSION")?Dice.normalize(input.expression()):input.expression();
        var command=new Command(input.requestId(),input.expectedVersion(),input.characterId(),input.key(),normalized,input.visibility(),List.copyOf(input.advantage()),List.copyOf(input.disadvantage()));
        return cards.actorLocked(actor,()->cards.locked(c,()->{
            var role=member(c,actor);require(!command.visibility().equals("DM")||role==Role.DM);
            CharacterService.Card card=null;
            if(!command.characterId().isEmpty()){
                UUID id;try{id=UUID.fromString(command.characterId());}catch(IllegalArgumentException e){throw CharacterSheet.invalid();}
                card=cards.card(c,id);if(card==null||role!=Role.DM&&(card.hidden()||card.importReview()&&!card.controllers().contains(actor)))throw missing();
                require(role==Role.DM||card.controllers().contains(actor));require(command.visibility().equals("DM")||!card.hidden()&&!card.importReview());
            }
            var fp=fingerprint(json,List.of(c,command.expectedVersion(),command.characterId(),command.key(),command.expression(),command.visibility(),command.advantage(),command.disadvantage()));var old=store.receipt(actor,command.requestId());
            if(old!=null){if(!old.fingerprint().equals(fp))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID already used");var result=store.roll(c,old.id());if(!visible(c,result,role))throw missing();return result;}
            AccountPolicy.version(card==null?cards.campaignVersion(c):card.version(),command.expectedVersion());
            Calculation calculation=new Calculation(0,0,List.of(),"");String expression;
            if(!command.key().equals("EXPRESSION")){
                if(card==null||!command.expression().isEmpty()||!shortcut(command.key()))throw CharacterSheet.invalid();
                calculation=card.sheet().calculate().get(command.key());if(calculation==null)throw CharacterSheet.invalid();
                int value=calculation.value();String modifier=value<0?Integer.toString(value):"+"+value;
                if(command.key().startsWith("damage:")){
                    var current=card;String attackId=command.key().substring(7);var attack=current.sheet().attacks().stream().filter(a->a.id().equals(attackId)).findFirst().orElseThrow(CharacterSheet::invalid);expression=attack.dice()+modifier;
                }else expression="1d20"+modifier;
            }else expression=command.expression();
            var result=Dice.roll(expression,command.advantage(),command.disadvantage(),entropy);
            var roll=new Roll(UUID.randomUUID(),command.characterId(),command.expectedVersion(),actor,card==null?actor:card.sheet().name(),clock.instant(),command.visibility(),command.key(),calculation,command.advantage(),command.disadvantage(),result);
            store.save(c,roll);store.receipt(c,actor,command.requestId(),new Receipt(fp,roll.id()));events.changed(c,command.visibility().equals("DM"));return roll;
        }));
    }
    private static boolean shortcut(String key){return key.equals("initiative")||key.matches("(ability|skill|save|attack|damage|spellAttack):.+");}
    /** Internal A05 plan only. No HTTP payload can choose critical dice counts. */
    Roll plannedDamage(UUID c,String actor,UUID request,CharacterService.Card card,String attackId,int count,int sides,int modifier){
        if(request==null)throw CharacterSheet.invalid();CharacterSheet.number(count,1,100);CharacterSheet.number(sides,2,1000);CharacterSheet.number(modifier,-10000,10000);
        return cards.actorLocked(actor,()->cards.locked(c,()->{
            require(member(c,actor)==Role.DM);var current=cards.card(c,card.id());if(current==null)throw missing();AccountPolicy.version(current.version(),card.version());
            String expression=count+"d"+sides+(modifier<0?"":"+")+modifier;
            String fp=fingerprint(json,List.of(c,"A05_DAMAGE",card.id(),card.version(),attackId,expression));var prior=store.receipt(actor,request);
            if(prior!=null){if(!prior.fingerprint().equals(fp))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID already used");return store.roll(c,prior.id());}
            var calculation=card.sheet().calculate().get("damage:"+attackId);if(calculation==null)throw CharacterSheet.invalid();
            var roll=new Roll(UUID.randomUUID(),card.id().toString(),card.version(),actor,card.sheet().name(),clock.instant(),"DM","damage:"+attackId,calculation,List.of(),List.of(),Dice.roll(expression,List.of(),List.of(),entropy));
            store.save(c,roll);store.receipt(c,actor,request,new Receipt(fp,roll.id()));events.changed(c,true);return roll;
        }));
    }
    public static String fingerprint(JsonMapper json,Object data){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(data).getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
