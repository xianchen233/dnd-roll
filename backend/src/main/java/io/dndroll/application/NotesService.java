package io.dndroll.application;

import io.dndroll.domain.*;
import io.dndroll.domain.Movement.Role;
import java.time.*;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** Collaborative notes and DM preparation have independent, immutable visibility. */
public final class NotesService {
    public record Create(UUID requestId,long expectedVersion,String visibility,String title,String text,List<UUID> attachments) {}
    public record Edit(UUID requestId,long expectedVersion,String title,String text,List<UUID> attachments,String reason) {}
    public record Note(UUID id,long version,String visibility,String title,String text,List<UUID> attachments,String actor,Instant at,Instant editedAt,String reason) {}
    public record View(UUID id,long version,String visibility,String title,String text,List<AssetService.Asset> attachments,String actor,Instant at,Instant editedAt) {}
    public record Revision(View note,String reason) {}
    public record Ack(UUID id,long version) {}
    public record Receipt(String fingerprint,Ack result) {}
    public record Listing(long campaignVersion,List<View> notes,String next) {}
    public record History(UUID noteId,List<Revision> revisions) {}
    public record Export(String format,int formatVersion,List<View> notes,List<History> histories,List<AssetService.Asset> attachments) {}
    public record Filter(String q,String visibility) {
        public Filter {CharacterSheet.text(q,100,false);if(!Set.of("","TEAM","DM").contains(visibility))throw CharacterSheet.invalid();}
    }
    public interface Store {
        Note note(UUID campaign,UUID id);void save(UUID campaign,Note note,boolean create);
        List<Note> history(UUID id);List<Note> list(UUID campaign,boolean dm,UUID before,Filter filter,int limit);
        Receipt receipt(String actor,UUID request);void receipt(UUID campaign,String actor,UUID request,Receipt receipt);
    }
    private final Store store;private final CharacterService.Store campaigns;private final DiceService.Events events;private final Clock clock;private final JsonMapper json;private final NoteImages images;
    public NotesService(Store store,CharacterService.Store campaigns,DiceService.Events events,Clock clock,JsonMapper json,NoteImages images){this.store=store;this.campaigns=campaigns;this.events=events;this.clock=clock;this.json=json;this.images=images;}
    private Role member(UUID c,String actor){var role=campaigns.role(c,actor);if(role==null)throw new DomainException("FORBIDDEN",403,"Membership required");return role;}
    private Note target(UUID c,UUID id,Role role){var note=store.note(c,id);if(note==null||note.visibility().equals("DM")&&role!=Role.DM)throw DiceService.missing();return note;}
    private void body(UUID request,long version,String title,String text,List<UUID> attachments){
        if(request==null||version<0)throw CharacterSheet.invalid();CharacterSheet.text(title,200,true);CharacterSheet.text(text,16000,false);
        if(attachments==null||attachments.size()>4||attachments.stream().anyMatch(Objects::isNull)||new HashSet<>(attachments).size()!=attachments.size())throw CharacterSheet.invalid();
    }
    private Receipt replay(String actor,UUID request,String fp){var old=store.receipt(actor,request);if(old!=null&&!old.fingerprint().equals(fp))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID already used");return old;}
    private Ack finish(UUID c,String actor,UUID request,String fp,Note n,boolean create){store.save(c,n,create);var ack=new Ack(n.id(),n.version());store.receipt(c,actor,request,new Receipt(fp,ack));events.changed(c,n.visibility().equals("DM"));return ack;}
    public Ack create(UUID c,String actor,Create command){
        body(command.requestId(),command.expectedVersion(),command.title(),command.text(),command.attachments());if(!Set.of("TEAM","DM").contains(command.visibility()))throw CharacterSheet.invalid();
        return campaigns.actorLocked(actor,()->campaigns.locked(c,()->{
            var role=member(c,actor);if(command.visibility().equals("DM"))AccountPolicy.dm(role);images.attach(c,null,command.attachments(),command.visibility(),actor);
            var fp=DiceService.fingerprint(json,List.of(c,"CREATE",command));var old=replay(actor,command.requestId(),fp);if(old!=null){target(c,old.result().id(),role);return old.result();}
            AccountPolicy.version(campaigns.campaignVersion(c),command.expectedVersion());var now=clock.instant();
            return finish(c,actor,command.requestId(),fp,new Note(UUID.randomUUID(),0,command.visibility(),command.title(),command.text(),List.copyOf(command.attachments()),actor,now,now,""),true);
        }));
    }
    public Ack edit(UUID c,UUID id,String actor,Edit command){
        body(command.requestId(),command.expectedVersion(),command.title(),command.text(),command.attachments());CharacterSheet.text(command.reason(),1000,true);
        return campaigns.actorLocked(actor,()->campaigns.locked(c,()->{
            var role=member(c,actor);var note=target(c,id,role);images.attach(c,id,command.attachments(),note.visibility(),actor);var fp=DiceService.fingerprint(json,List.of(c,id,"EDIT",command));var old=replay(actor,command.requestId(),fp);if(old!=null)return old.result();
            AccountPolicy.version(note.version(),command.expectedVersion());
            return finish(c,actor,command.requestId(),fp,new Note(id,note.version()+1,note.visibility(),command.title(),command.text(),List.copyOf(command.attachments()),actor,note.at(),clock.instant(),command.reason()),false);
        }));
    }
    private View view(UUID c,Note n){return new View(n.id(),n.version(),n.visibility(),n.title(),n.text(),n.attachments().stream().map(id->images.metadata(c,id)).toList(),n.actor(),n.at(),n.editedAt());}
    private List<Revision> revisions(UUID c,UUID id){return store.history(id).stream().map(n->new Revision(view(c,n),n.reason())).toList();}
    public View detail(UUID c,UUID id,String actor){return campaigns.locked(c,()->view(c,target(c,id,member(c,actor))));}
    public List<Revision> history(UUID c,UUID id,String actor){return campaigns.locked(c,()->{target(c,id,member(c,actor));return revisions(c,id);});}
    public Listing list(UUID c,String actor,UUID before,Filter filter){return campaigns.locked(c,()->{
        var role=member(c,actor);if(before!=null)target(c,before,role);var rows=store.list(c,role==Role.DM,before,filter,51);var page=rows.subList(0,Math.min(50,rows.size()));return new Listing(campaigns.campaignVersion(c),page.stream().map(n->view(c,n)).toList(),rows.size()>50?page.getLast().id().toString():"");
    });}
    public Export export(UUID c,String actor,Filter filter){return campaigns.locked(c,()->{
        var role=member(c,actor);var notes=new ArrayList<Note>();UUID before=null;
        do{var page=store.list(c,role==Role.DM,before,filter,500);notes.addAll(page);before=page.size()==500?page.getLast().id():null;}while(before!=null);
        Collections.reverse(notes);var histories=notes.stream().map(n->new History(n.id(),revisions(c,n.id()))).toList();var manifest=new LinkedHashMap<UUID,AssetService.Asset>();for(var h:histories)for(var r:h.revisions())for(var image:r.note().attachments())manifest.put(image.id(),image);return new Export("dnd-roll.notes",1,notes.stream().map(n->view(c,n)).toList(),histories,List.copyOf(manifest.values()));
    });}
    public String markdown(Export data){var out=new StringBuilder("# DND ROLL · 团队笔记与 DM 资料\n\n");for(var note:data.notes()){indent(out,note.title()+" · "+note.visibility()+" · "+note.id()+"\n"+note.text());for(var h:data.histories())if(h.noteId().equals(note.id()))for(var r:h.revisions())indent(out,"版本 "+r.note().version()+" · "+r.note().actor()+" · "+r.note().editedAt()+" · "+r.reason()+"\n"+r.note().title()+"\n"+r.note().text());}out.append("## 图片附件清单\n\n");for(var image:data.attachments())indent(out,image.id()+" · "+image.mediaType()+" · "+image.byteSize()+" bytes");return out.toString();}
    private void indent(StringBuilder out,String text){for(String line:text.split("\\R",-1))out.append("    ").append(line.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")).append('\n');out.append('\n');}
}
