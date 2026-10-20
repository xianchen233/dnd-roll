package io.dndroll.application;

import io.dndroll.domain.*;
import io.dndroll.domain.CharacterSheet.*;
import io.dndroll.domain.Movement.Role;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Character resources/history are independent of map tokens and their control. */
public final class CharacterService {
    public record Card(UUID id,UUID campaignId,long version,boolean hidden,boolean shareFull,boolean importReview,List<String> controllers,CharacterSheet sheet) {}
    public record Ack(UUID id,long version) {}
    public record Summary(UUID id,String name,String avatar,long version,boolean canEdit,boolean canReadFull,String health,int ac,int speed,List<String> conditions) {}
    public record Listing(long campaignVersion,List<Summary> characters) {}
    public record Detail(UUID id,long version,boolean canEdit,boolean canManage,CharacterSheet sheet,Map<String,Calculation> calculations,List<String> expiredConditionIds) {}
    public record Access(long version,boolean hidden,boolean shareFull,boolean importReview,List<String> controllers) {}
    public record History(long version,String actor,String kind,String reason,Instant at,Card before,Card after) {}
    public record Document(String format,int formatVersion,String rules,CharacterSheet sheet) {}
    public record Receipt(String fingerprint,Ack result) {}
    public record Placement(UUID tokenId,UUID sceneId,long tokenVersion) {}
    public interface Store {
        <T>T actorLocked(String actor,Supplier<T> work); <T>T locked(UUID campaign,Supplier<T> work);
        Role role(UUID campaign,String actor); long campaignVersion(UUID campaign); void advanceCampaign(UUID campaign);
        List<Card> cards(UUID campaign); Card card(UUID campaign,UUID id); void save(Card card,boolean create);
        List<History> history(UUID id); void history(History history);
        Receipt receipt(String actor,UUID request); void receipt(String actor,UUID request,UUID campaign,Receipt receipt);
        List<Placement> placements(UUID campaign,UUID id); void link(UUID campaign,UUID id,UUID token,long tokenVersion);
    }
    private final Store store;private final SceneService.Events events;private final Clock clock;private final JsonMapper json;
    public CharacterService(Store store,SceneService.Events events,Clock clock,JsonMapper json){this.store=store;this.events=events;this.clock=clock;this.json=json;}
    public Listing list(UUID c,String actor){return store.locked(c,()->{var role=member(c,actor);return new Listing(store.campaignVersion(c),store.cards(c).stream().filter(s->visible(s,role,actor)).map(s->new Summary(s.id(),s.sheet().name(),s.sheet().avatar(),s.version(),editable(s,role,actor),full(s,role,actor),s.sheet().combat().hp()==0?"DOWN":s.sheet().combat().hp()==s.sheet().combat().maxHp()?"UNHURT":"HURT",s.sheet().calculate().get("ac").value(),s.sheet().combat().speed(),s.sheet().conditions().stream().map(Condition::name).toList())).toList());});}
    public Detail detail(UUID c,UUID id,String actor){return store.locked(c,()->{var role=member(c,actor);var card=target(c,id,role,actor);require(full(card,role,actor));return new Detail(id,card.version(),editable(card,role,actor),role==Role.DM,card.sheet(),card.sheet().calculate(),role==Role.DM?card.sheet().conditions().stream().filter(this::expired).map(Condition::id).toList():List.of());});}
    public Access access(UUID c,UUID id,String actor){return store.locked(c,()->{var role=member(c,actor);require(role==Role.DM);var card=target(c,id,role,actor);return new Access(card.version(),card.hidden(),card.shareFull(),card.importReview(),card.controllers());});}
    public List<History> history(UUID c,UUID id,String actor){return store.locked(c,()->{var role=member(c,actor);var card=target(c,id,role,actor);require(editable(card,role,actor));return store.history(id);});}
    public Document export(UUID c,UUID id,String actor){var d=detail(c,id,actor);return new Document("dnd-roll.character",1,"5R",d.sheet());}
    public List<Placement> placements(UUID c,UUID id,String actor){return store.locked(c,()->{var role=member(c,actor);require(role==Role.DM);target(c,id,role,actor);return store.placements(c,id);});}
    public Ack create(UUID c,String actor,UUID request,long expected,CharacterSheet sheet){return execute("CREATE",c,null,actor,request,expected,sheet,"创建角色",(card,role)->{
        AccountPolicy.version(store.campaignVersion(c),expected);sheet.validate();require(role==Role.DM||sheet.overrides().isEmpty());store.advanceCampaign(c);return new Card(UUID.randomUUID(),c,0,false,false,false,List.of(actor),sheet);
    });}
    public Ack update(UUID c,UUID id,String actor,UUID request,long expected,CharacterSheet sheet,String reason){return execute("UPDATE",c,id,actor,request,expected,List.of(sheet,reason),reason,(card,role)->{
        AccountPolicy.version(card.version(),expected);sheet.validate();if(role!=Role.DM){require(card.sheet().overrides().equals(sheet.overrides()));for(var old:card.sheet().conditions())if(expired(old))require(sheet.conditions().stream().anyMatch(old::equals));}
        return next(card,sheet);
    });}
    public Ack access(UUID c,UUID id,String actor,UUID request,long expected,List<String> controllers,boolean hidden,boolean share,boolean approve,String reason){
        if(controllers==null||controllers.size()>100||controllers.stream().anyMatch(Objects::isNull)||new HashSet<>(controllers).size()!=controllers.size())throw CharacterSheet.invalid();var sorted=controllers.stream().sorted().toList();sorted.forEach(n->CharacterSheet.text(n,80,true));
        return execute("ACCESS",c,id,actor,request,expected,List.of(sorted,hidden,share,approve,reason),reason,(card,role)->{
            AccountPolicy.version(card.version(),expected);for(String user:sorted)if(store.role(c,user)==null)throw CharacterSheet.invalid();
            return new Card(id,c,card.version()+1,hidden,share,card.importReview()&&!approve,sorted,card.sheet());
        });
    }
    public Ack resource(UUID c,UUID id,String actor,UUID request,long expected,String resource,int delta,String reason){return execute("RESOURCE",c,id,actor,request,expected,List.of(resource,delta,reason),reason,(card,role)->{
        AccountPolicy.version(card.version(),expected);CharacterSheet.number(delta,-100000,100000);if(delta==0)throw CharacterSheet.invalid();var s=card.sheet();var combat=s.combat();var resources=s.resources();
        if(resource.equals("hp")||resource.equals("tempHp")){
            if(resource.equals("tempHp")&&delta>0)throw CharacterSheet.invalid();
            int hp=combat.hp()+(resource.equals("hp")?delta:0),temp=combat.tempHp()+(resource.equals("tempHp")?delta:0);
            combat=new Combat(combat.maxHp(),hp,temp,combat.speed(),combat.deathSuccesses(),combat.deathFailures(),combat.acMode(),combat.manualAc(),combat.source());
        }else{
            if(resources.stream().noneMatch(r->r.id().equals(resource)))throw CharacterSheet.invalid();resources=resources.stream().map(r->r.id().equals(resource)?new Resource(r.id(),r.name(),r.kind(),r.current()+delta,r.max(),r.recovery(),r.source()):r).toList();
        }
        var changed=s.change(s.name(),combat,resources,s.conditions(),s.overrides());changed.validate();return next(card,changed);
    });}
    public Ack condition(UUID c,UUID id,String actor,UUID request,long expected,String action,String conditionId,String reason){return execute("CONDITION",c,id,actor,request,expected,Arrays.asList(action,conditionId,reason),reason,(card,role)->{
        AccountPolicy.version(card.version(),expected);var s=card.sheet();List<Condition> conditions;
        if("TICK".equals(action)&&conditionId==null)conditions=s.conditions().stream().map(v->new Condition(v.id(),v.name(),v.icon(),v.description(),v.source(),v.remainingRounds()==null?null:Math.max(0,v.remainingRounds()-1),v.expiresAt())).toList();
        else if("REMOVE".equals(action)&&conditionId!=null&&s.conditions().stream().anyMatch(v->v.id().equals(conditionId)))conditions=s.conditions().stream().filter(v->!v.id().equals(conditionId)).toList();else throw CharacterSheet.invalid();
        return next(card,s.change(s.name(),s.combat(),s.resources(),conditions,s.overrides()));
    });}
    public Ack copy(UUID c,UUID id,String actor,UUID request,long expected,String name){return execute("COPY",c,id,actor,request,expected,name,"复制角色",(card,role)->{
        AccountPolicy.version(card.version(),expected);CharacterSheet.text(name,100,true);var s=card.sheet();store.advanceCampaign(c);return new Card(UUID.randomUUID(),c,0,false,false,card.importReview(),List.of(actor),s.change(name,s.combat(),s.resources(),s.conditions(),List.of()));
    });}
    public Ack importDocument(UUID c,String actor,UUID request,long expected,Document doc){return execute("IMPORT",c,null,actor,request,expected,doc,"结构化导入；重新归属并清除旧覆盖",(card,role)->{
        if(doc==null||!"dnd-roll.character".equals(doc.format())||doc.formatVersion()!=1||!"5R".equals(doc.rules())||doc.sheet()==null)throw CharacterSheet.invalid();AccountPolicy.version(store.campaignVersion(c),expected);doc.sheet().validate();var s=doc.sheet();store.advanceCampaign(c);return new Card(UUID.randomUUID(),c,0,false,false,role!=Role.DM,List.of(actor),s.change(s.name(),s.combat(),s.resources(),s.conditions(),List.of()));
    });}
    public Ack link(UUID c,UUID id,String actor,UUID request,long expected,UUID token,long tokenVersion,String reason){return execute("LINK",c,id,actor,request,expected,List.of(token,tokenVersion,reason),reason,(card,role)->{
        AccountPolicy.version(card.version(),expected);CharacterSheet.number(tokenVersion,0,Long.MAX_VALUE);store.link(c,id,token,tokenVersion);return next(card,card.sheet());
    });}
    private interface Mutation{Card apply(Card card,Role role);}
    private Ack execute(String kind,UUID c,UUID id,String actor,UUID request,long expected,Object payload,String reason,Mutation change){
        if(request==null||expected<0||payload==null)throw CharacterSheet.invalid();CharacterSheet.text(reason,1000,true);
        return store.actorLocked(actor,()->store.locked(c,()->{
            var role=member(c,actor);Card current=id==null?null:target(c,id,role,actor);
            if(Set.of("ACCESS","CONDITION","LINK").contains(kind))require(role==Role.DM);
            else if(current!=null)require(kind.equals("COPY")?full(current,role,actor):editable(current,role,actor));
            var fingerprint=fingerprint(Arrays.asList(kind,c,id,expected,payload));var receipt=store.receipt(actor,request);
            if(receipt!=null){
                if(!receipt.fingerprint().equals(fingerprint))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID already used");
                // Creation/copy/import may acknowledge a different identity from
                // the command target. Recheck its current visibility as well.
                target(c,receipt.result().id(),role,actor);
                return receipt.result();
            }
            var next=change.apply(current,role);boolean create=current==null||!current.id().equals(next.id());store.save(next,create);
            store.history(new History(next.version(),actor,kind,reason,clock.instant(),create?null:current,next));var ack=new Ack(next.id(),next.version());store.receipt(actor,request,c,new Receipt(fingerprint,ack));events.changed(c);return ack;
        }));
    }
    private Card next(Card c,CharacterSheet s){return new Card(c.id(),c.campaignId(),c.version()+1,c.hidden(),c.shareFull(),c.importReview(),c.controllers(),s);}
    private Role member(UUID c,String actor){var r=store.role(c,actor);require(r!=null);return r;}
    private Card target(UUID c,UUID id,Role role,String actor){var v=store.card(c,id);if(v==null||!visible(v,role,actor))throw new DomainException("NOT_FOUND",404,"Character not found");return v;}
    private boolean visible(Card c,Role r,String actor){return r==Role.DM||(!c.hidden()&&(!c.importReview()||c.controllers().contains(actor)));}
    private boolean editable(Card c,Role r,String actor){return r==Role.DM||c.controllers().contains(actor);}
    private boolean full(Card c,Role r,String actor){return editable(c,r,actor)||c.shareFull();}
    private boolean expired(Condition c){return(c.remainingRounds()!=null&&c.remainingRounds()==0)||(c.expiresAt()!=null&&!c.expiresAt().isAfter(clock.instant()));}
    private static void require(boolean allowed){if(!allowed)throw new DomainException("FORBIDDEN",403,"Character permission required");}
    private String fingerprint(Object value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(canonical(json.valueToTree(value))).getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private JsonNode canonical(JsonNode n){if(n.isObject()){var o=json.createObjectNode();n.properties().stream().sorted(Map.Entry.comparingByKey()).forEach(e->o.set(e.getKey(),canonical(e.getValue())));return o;}if(n.isArray()){var a=json.createArrayNode();n.forEach(v->a.add(canonical(v)));return a;}return n;}
}
