package io.dndroll.application;

import io.dndroll.domain.*;
import io.dndroll.domain.Movement.Role;
import java.time.*;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** Long-lived authored messages; all reads and retries authorize current identities. */
public final class TimelineService {
    public record Send(UUID requestId,long expectedVersion,String characterId,String kind,String text,String replyTo,List<UUID> attachments) {}
    public record Edit(UUID requestId,long expectedVersion,String text,List<UUID> attachments,String reason) {}
    public record Message(UUID id,long version,String actor,String characterId,long sourceVersion,String name,String kind,String text,String replyTo,List<UUID> attachments,Instant at,Instant editedAt,String reason) {}
    public record Ack(UUID id,long version) {}
    public record Receipt(String fingerprint,Ack result) {}
    public record Entry(UUID id,String kind,Instant at,String sessionId,Object data,Object reply) {}
    public record Listing(List<Entry> entries,String next) {}
    public record Row(UUID id,String kind,Instant at,String sessionId) {}
    public record Revision(long version,String actor,String name,String text,List<AssetService.Asset> attachments,Instant at,String reason) {}
    public record Session(UUID id,String name,boolean archived) {}
    public record Sessions(long campaignVersion,List<Session> sessions) {}
    public record SessionCommand(UUID requestId,long expectedVersion,String kind,String name) {}
    public record HistoryExport(UUID entryId,List<Revision> revisions) {}
    public record Export(String format,int formatVersion,List<Entry> entries,List<HistoryExport> histories,List<AssetService.Asset> attachments) {}
    public record Filter(String q,String kind,String characterId,String sessionId,String from,String to) {
        public Filter {
            CharacterSheet.text(q,100,false);
            if(!Set.of("","RP","OOC","DICE","ACTION").contains(kind))throw CharacterSheet.invalid();
            if(!characterId.isEmpty())uuid(characterId);if(!sessionId.isEmpty())uuid(sessionId);
            for(String time:List.of(from,to))if(!time.isEmpty())try{if(!time.endsWith("Z"))throw CharacterSheet.invalid();Instant.parse(time);}catch(java.time.format.DateTimeParseException e){throw CharacterSheet.invalid();}
            if(!from.isEmpty()&&!to.isEmpty()&&Instant.parse(from).isAfter(Instant.parse(to)))throw CharacterSheet.invalid();
        }
    }
    public interface Store {
        Message message(UUID c,UUID id);void save(UUID c,Message m,boolean create);
        List<Message> history(UUID id);
        Row row(UUID c,UUID id);boolean readable(UUID c,UUID id,boolean dm);List<Row> list(UUID c,boolean dm,UUID before,Filter filter,int limit);
        List<Session> sessions(UUID c);Session session(UUID c,SessionCommand command);
        Receipt receipt(String ns,String actor,UUID id);void receipt(String ns,UUID c,String actor,UUID id,Receipt receipt);
    }
    private final Store store;private final CharacterService.Store cards;private final DiceService.Events events;private final Clock clock;private final JsonMapper json;private final TimelineImages images;private final DiceService dice;private final CombatService combat;
    public TimelineService(Store store,CharacterService.Store cards,DiceService.Events events,Clock clock,JsonMapper json,TimelineImages images,DiceService dice,CombatService combat){this.store=store;this.cards=cards;this.events=events;this.clock=clock;this.json=json;this.images=images;this.dice=dice;this.combat=combat;}
    private Role member(UUID c,String actor){var r=cards.role(c,actor);require(r!=null);return r;}
    private static void require(boolean ok){if(!ok)throw new DomainException("FORBIDDEN",403,"Message permission required");}
    private boolean visible(UUID c,Message m,Role role){if(m==null)return false;if(role==Role.DM||m.characterId().isEmpty())return true;var card=cards.card(c,uuid(m.characterId()));return card!=null&&!card.hidden()&&!card.importReview();}
    private Message target(UUID c,UUID id,Role role){var m=store.message(c,id);if(!visible(c,m,role))throw DiceService.missing();return m;}
    private CharacterService.Card source(UUID c,String id,String actor,Role role){if(id.isEmpty())return null;var v=cards.card(c,uuid(id));if(v==null||role!=Role.DM&&(v.hidden()||v.importReview()))throw DiceService.missing();require(role==Role.DM||v.controllers().contains(actor));return v;}
    public static UUID uuid(String id){try{var u=UUID.fromString(id);if(!u.toString().equals(id))throw CharacterSheet.invalid();return u;}catch(IllegalArgumentException e){throw CharacterSheet.invalid();}}
    private void command(UUID request,long version){if(request==null||version<0)throw CharacterSheet.invalid();}
    private void body(String text,List<UUID> images){CharacterSheet.text(text,8000,false);if(images==null||images.size()>4||images.stream().anyMatch(Objects::isNull)||new HashSet<>(images).size()!=images.size()||text.isBlank()&&images.isEmpty())throw CharacterSheet.invalid();}
    private Receipt replay(String actor,UUID request,String fp){var r=store.receipt("messages",actor,request);if(r!=null&&!r.fingerprint().equals(fp))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID already used");return r;}
    private Ack finish(UUID c,String actor,UUID request,String fp,Message m){var ack=new Ack(m.id(),m.version());store.receipt("messages",c,actor,request,new Receipt(fp,ack));var card=m.characterId().isEmpty()?null:cards.card(c,uuid(m.characterId()));events.changed(c,card!=null&&(card.hidden()||card.importReview()));return ack;}
    public Ack send(UUID c,String actor,Send s){
        command(s.requestId(),s.expectedVersion());CharacterSheet.text(s.characterId(),36,false);CharacterSheet.text(s.replyTo(),36,false);body(s.text(),s.attachments());
        if(!Set.of("RP","OOC").contains(s.kind())||s.kind().equals("RP")==s.characterId().isEmpty())throw CharacterSheet.invalid();
        return cards.actorLocked(actor,()->cards.locked(c,()->{
            var role=member(c,actor);var card=source(c,s.characterId(),actor,role);if(!s.replyTo().isEmpty())readEntry(c,uuid(s.replyTo()),role,actor);images.attach(c,s.attachments(),actor);
            String fp=DiceService.fingerprint(json,List.of(c,"SEND",s));var old=replay(actor,s.requestId(),fp);if(old!=null){target(c,old.result().id(),role);return old.result();}
            AccountPolicy.version(card==null?cards.campaignVersion(c):card.version(),s.expectedVersion());
            var now=clock.instant();var m=new Message(UUID.randomUUID(),0,actor,s.characterId(),s.expectedVersion(),card==null?actor:card.sheet().name(),s.kind(),s.text(),s.replyTo(),List.copyOf(s.attachments()),now,now,"");store.save(c,m,true);return finish(c,actor,s.requestId(),fp,m);
        }));
    }
    public Ack edit(UUID c,UUID id,String actor,Edit e){
        command(e.requestId(),e.expectedVersion());body(e.text(),e.attachments());CharacterSheet.text(e.reason(),1000,true);
        return cards.actorLocked(actor,()->cards.locked(c,()->{
            var role=member(c,actor);var old=target(c,id,role);require(old.actor().equals(actor));source(c,old.characterId(),actor,role);images.attach(c,e.attachments(),actor);
            String fp=DiceService.fingerprint(json,List.of(c,id,"EDIT",e));var previous=replay(actor,e.requestId(),fp);if(previous!=null)return previous.result();AccountPolicy.version(old.version(),e.expectedVersion());
            var next=new Message(id,old.version()+1,old.actor(),old.characterId(),old.sourceVersion(),old.name(),old.kind(),e.text(),old.replyTo(),List.copyOf(e.attachments()),old.at(),clock.instant(),e.reason());store.save(c,next,false);return finish(c,actor,e.requestId(),fp,next);
        }));
    }
    private Object projection(UUID c,Row row,Role role,String actor){
        if(row.kind().equals("DICE"))return dice.detail(c,row.id(),actor);
        if(row.kind().equals("ACTION"))return combat.detail(c,row.id(),actor);
        var m=target(c,row.id(),role);var view=new LinkedHashMap<String,Object>();view.put("version",m.version());view.put("actor",m.actor());view.put("characterId",m.characterId());view.put("sourceVersion",m.sourceVersion());view.put("name",m.name());view.put("text",m.text());view.put("attachments",m.attachments().stream().map(id->images.metadata(c,id)).toList());view.put("edited",m.version()>0);var card=m.characterId().isEmpty()?null:cards.card(c,uuid(m.characterId()));view.put("canEdit",m.actor().equals(actor)&&(card==null||role==Role.DM||card.controllers().contains(actor)));return view;
    }
    private Entry readEntry(UUID c,UUID id,Role role,String actor){
        var row=store.row(c,id);if(row==null||!store.readable(c,id,role==Role.DM))throw DiceService.missing();Object data=projection(c,row,role,actor),reply=null;
        if(row.kind().equals("RP")||row.kind().equals("OOC")){var m=target(c,id,role);if(!m.replyTo().isEmpty()){UUID quoteId=uuid(m.replyTo());var quoted=store.row(c,quoteId);if(quoted!=null&&store.readable(c,quoteId,role==Role.DM))reply=Map.of("id",quoted.id(),"kind",quoted.kind());}}
        return new Entry(id,row.kind(),row.at(),row.sessionId(),data,reply);
    }
    private List<Revision> revisions(UUID c,UUID id){return store.history(id).stream().map(m->new Revision(m.version(),m.actor(),m.name(),m.text(),m.attachments().stream().map(image->images.metadata(c,image)).toList(),m.editedAt(),m.reason())).toList();}
    public Entry detail(UUID c,UUID id,String actor){return cards.locked(c,()->readEntry(c,id,member(c,actor),actor));}
    public List<Revision> history(UUID c,UUID id,String actor){return cards.locked(c,()->{target(c,id,member(c,actor));return revisions(c,id);});}
    public Listing list(UUID c,String actor,UUID before,Filter filter){return cards.locked(c,()->{var role=member(c,actor);if(before!=null)readEntry(c,before,role,actor);var rows=store.list(c,role==Role.DM,before,filter,51);var page=rows.subList(0,Math.min(50,rows.size()));return new Listing(page.stream().map(row->readEntry(c,row.id(),role,actor)).toList(),rows.size()>50?page.getLast().id().toString():"");});}
    public Sessions sessions(UUID c,String actor){return cards.locked(c,()->{member(c,actor);return new Sessions(cards.campaignVersion(c),store.sessions(c));});}
    public Ack session(UUID c,String actor,SessionCommand s){command(s.requestId(),s.expectedVersion());CharacterSheet.text(s.name(),100,s.kind().equals("START"));if(!Set.of("START","ARCHIVE").contains(s.kind())||s.kind().equals("ARCHIVE")&&!s.name().isEmpty())throw CharacterSheet.invalid();return cards.actorLocked(actor,()->cards.locked(c,()->{AccountPolicy.dm(member(c,actor));String fp=DiceService.fingerprint(json,List.of(c,s));var previous=store.receipt("sessions",actor,s.requestId());if(previous!=null){if(!previous.fingerprint().equals(fp))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID already used");return previous.result();}AccountPolicy.version(cards.campaignVersion(c),s.expectedVersion());var session=store.session(c,s);cards.advanceCampaign(c);var ack=new Ack(session.id(),cards.campaignVersion(c));store.receipt("sessions",c,actor,s.requestId(),new Receipt(fp,ack));events.changed(c,false);return ack;}));}
    public Export export(UUID c,String actor,Filter filter){return cards.locked(c,()->{
        var role=member(c,actor);var entries=new ArrayList<Entry>();UUID before=null;
        do{var rows=store.list(c,role==Role.DM,before,filter,500);for(var row:rows)entries.add(readEntry(c,row.id(),role,actor));before=rows.size()==500?rows.getLast().id():null;}while(before!=null);
        Collections.reverse(entries);var histories=new ArrayList<HistoryExport>();var manifest=new LinkedHashMap<UUID,AssetService.Asset>();
        for(var e:entries)if(e.kind().equals("RP")||e.kind().equals("OOC")){var h=revisions(c,e.id());histories.add(new HistoryExport(e.id(),h));for(var rev:h)for(var image:rev.attachments())manifest.put(image.id(),image);}
        return new Export("dnd-roll.timeline",1,List.copyOf(entries),histories,List.copyOf(manifest.values()));
    });}
    public String markdown(Export data){var out=new StringBuilder("# DND ROLL · 时间线\n\n");for(var entry:data.entries()){var node=json.valueToTree(entry.data());out.append("## ").append(entry.at()).append(" · ").append(entry.kind()).append("\n\n");String content=node.has("text")?node.path("name").asString()+" ("+node.path("actor").asString()+")\n"+node.path("text").asString():json.writeValueAsString(entry.data());indent(out,content);for(var h:data.histories())if(h.entryId().equals(entry.id()))for(var rev:h.revisions())if(h.revisions().size()>1)indent(out,"编辑版本 "+rev.version()+" · "+rev.actor()+" · "+rev.at()+" · "+rev.reason()+"\n"+rev.text());}out.append("## 图片附件清单\n\n");for(var image:data.attachments())indent(out,image.id()+" · "+image.mediaType()+" · "+image.byteSize()+" bytes");return out.toString();}
    private void indent(StringBuilder out,String text){for(String line:text.split("\\R",-1))out.append("    ").append(line.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")).append('\n');out.append('\n');}
}
