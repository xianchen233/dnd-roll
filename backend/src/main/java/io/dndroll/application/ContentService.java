package io.dndroll.application;

import io.dndroll.domain.*;
import io.dndroll.domain.Movement.Role;
import java.util.*;
import java.util.function.Supplier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import tools.jackson.databind.json.JsonMapper;

public final class ContentService {
    public record Policy(String mode,List<UUID> approvedIds) {}
    public record View(UUID id,String name,List<String> aliases,String type,CharacterSheet.Source source,String description,int spellLevel,String dice,String damageType,String rights,String license,String attribution,String changes,boolean admitted,String support) {}
    public record Listing(long campaignVersion,String mode,List<UUID> approvedIds,List<View> entries,int total,int structured,int automated) {}
    public record Ack(UUID id,long version) {}
    public record Receipt(String fingerprint,Ack result) {}
    public record Preview(UUID entryId,long expectedVersion,long policyVersion,CharacterSheet sheet,String warning) {}
    public interface Store {
        List<ContentEntry> entries(UUID c);Policy policy(UUID c);void policy(UUID c,Policy p);void entry(UUID c,ContentEntry e);
        Receipt receipt(String actor,UUID request);void receipt(String actor,UUID request,UUID c,Receipt r);
    }
    private final Store store;private final CharacterService.Store cards;private final CharacterService characters;private final SceneService.Events events;private final JsonMapper json;private final List<ContentEntry> builtin;
    public ContentService(Store store,CharacterService.Store cards,CharacterService characters,SceneService.Events events,JsonMapper json,List<ContentEntry> builtin){this.store=store;this.cards=cards;this.characters=characters;this.events=events;this.json=json;this.builtin=List.copyOf(builtin);builtin.forEach(e->e.data().validate());}
    private Role member(UUID c,String actor){var r=cards.role(c,actor);require(r!=null);return r;}
    private static void require(boolean ok){if(!ok)throw new DomainException("FORBIDDEN",403,"Content permission required");}
    private List<ContentEntry> all(UUID c){var entries=new ArrayList<>(builtin);entries.addAll(store.entries(c));return entries;}
    private boolean admitted(ContentEntry e,Policy p,Role r){return r==Role.DM||p.mode().equals("FREE")||e.builtin()||p.approvedIds().contains(e.id());}
    private ContentEntry entry(UUID c,UUID id){return all(c).stream().filter(e->e.id().equals(id)).findFirst().orElseThrow(()->new DomainException("NOT_FOUND",404,"Content not found"));}
    public Listing list(UUID c,String actor,String q,String type,String source,String rules,String allowed){
        CharacterSheet.text(q,100,false);CharacterSheet.text(source,100,false);if(!type.isEmpty()&&!ContentEntry.TYPES.contains(type)||!rules.isEmpty()&&!Set.of("5R","2014","CUSTOM").contains(rules)||!Set.of("","true","false").contains(allowed))throw CharacterSheet.invalid();
        return cards.locked(c,()->{var role=member(c,actor);var policy=store.policy(c);var all=all(c);
            var filtered=all.stream().filter(e->{var d=e.data();String hay=(d.name()+" "+String.join(" ",d.aliases())+" "+d.description()+" "+d.source().book()).toLowerCase(Locale.ROOT);return hay.contains(q.toLowerCase(Locale.ROOT))&&(type.isEmpty()||d.type().equals(type))&&d.source().book().toLowerCase(Locale.ROOT).contains(source.toLowerCase(Locale.ROOT))&&(rules.isEmpty()||d.source().rules().equals(rules))&&(allowed.isEmpty()||Boolean.toString(admitted(e,policy,Role.PLAYER)).equals(allowed));})
            .sorted(Comparator.comparing((ContentEntry e)->e.data().name()).thenComparing(ContentEntry::id)).map(e->{var d=e.data();return new View(e.id(),d.name(),d.aliases(),d.type(),d.source(),d.description(),d.spellLevel(),d.dice(),d.damageType(),d.rights(),e.license(),e.attribution(),e.changes(),admitted(e,policy,Role.PLAYER),"DESCRIPTION");}).toList();
            return new Listing(cards.campaignVersion(c),policy.mode(),policy.approvedIds(),filtered,all.size(),(int)all.stream().filter(e->!e.data().type().equals("RULE")).count(),0);
        });
    }
    public Ack publish(UUID c,String actor,UUID request,long expected,ContentEntry.Data data){
        data.validate();return mutate(c,actor,request,expected,List.of("CUSTOM",data),false,()->{if(store.entries(c).size()>=500)throw CharacterSheet.invalid();var e=new ContentEntry(UUID.randomUUID(),data,false,"CAMPAIGN","发布者声明有权与本团分享","本团自定义；不代表官方规则");store.entry(c,e);return e.id();});
    }
    public Ack settings(UUID c,String actor,UUID request,long expected,String mode,List<UUID> ids){
        if(!Set.of("REVIEW","FREE").contains(mode)||ids==null||ids.size()>500||ids.contains(null)||new HashSet<>(ids).size()!=ids.size())throw CharacterSheet.invalid();var sorted=ids.stream().sorted().toList();
        return mutate(c,actor,request,expected,List.of("SETTINGS",mode,sorted),true,()->{var custom=store.entries(c).stream().map(ContentEntry::id).toList();if(!custom.containsAll(ids))throw CharacterSheet.invalid();store.policy(c,new Policy(mode,sorted));return c;});
    }
    private Ack mutate(UUID c,String actor,UUID request,long expected,Object payload,boolean dm,Supplier<UUID> work){
        if(request==null||expected<0)throw CharacterSheet.invalid();return cards.actorLocked(actor,()->cards.locked(c,()->{
            var role=member(c,actor);require(!dm||role==Role.DM);var fp=fingerprint(List.of(c,expected,payload));var old=store.receipt(actor,request);if(old!=null){if(!fp.equals(old.fingerprint()))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID already used");return old.result();}
            AccountPolicy.version(cards.campaignVersion(c),expected);UUID result=work.get();cards.advanceCampaign(c);var ack=new Ack(result,cards.campaignVersion(c));store.receipt(actor,request,c,new Receipt(fp,ack));events.changed(c);return ack;
        }));
    }
    public Preview preview(UUID c,UUID id,String actor,long expected,UUID entryId,String ability,String casting){return cards.locked(c,()->{
        var d=characters.detail(c,id,actor);require(d.canEdit());var e=entry(c,entryId);require(admitted(e,store.policy(c),member(c,actor)));AccountPolicy.version(d.version(),expected);
        return new Preview(entryId,expected,cards.campaignVersion(c),e.fill(d.sheet(),ability,casting),"仅填字段；物种/背景将替换说明。属性、熟练、精通、资源与效果需手动选择；不执行规则。");
    });}
    public CharacterService.Ack fill(UUID c,UUID id,String actor,UUID request,long expected,long policy,UUID entryId,String ability,String casting,String reason){return cards.actorLocked(actor,()->cards.locked(c,()->{
        // Reauthorize admission even on an old receipt. CharacterService rechecks target/control.
        require(characters.detail(c,id,actor).canEdit());
        var role=member(c,actor);var e=entry(c,entryId);require(admitted(e,store.policy(c),role));
        return characters.fillContent(c,id,actor,request,expected,List.of(policy,entryId,ability,casting,reason),reason,s->{AccountPolicy.version(cards.campaignVersion(c),policy);return e.fill(s,ability,casting);});
    }));}
    private String fingerprint(Object v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(v).getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
