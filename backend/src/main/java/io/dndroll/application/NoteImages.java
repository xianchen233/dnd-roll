package io.dndroll.application;

import io.dndroll.domain.*;
import io.dndroll.domain.Movement.Role;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** Durable note images are separate from maps and RP, and never change visibility. */
public final class NoteImages {
    public record Image(AssetService.Asset metadata,String actor,String visibility,String storageKey,String sha256) {}
    public interface Store {
        Image image(UUID c,UUID id);void saveImage(UUID c,Image image);
        boolean readableImage(UUID c,UUID id,String actor,boolean dm);boolean referencedBy(UUID c,UUID image,UUID note);
        NotesService.Receipt uploadReceipt(String actor,UUID id);void uploadReceipt(UUID c,String actor,UUID id,NotesService.Receipt receipt);
    }
    private final Store store;private final CharacterService.Store campaigns;private final AssetService.Files files;private final JsonMapper json;
    public NoteImages(Store store,CharacterService.Store campaigns,AssetService.Files files,JsonMapper json){this.store=store;this.campaigns=campaigns;this.files=files;this.json=json;}
    private Role member(UUID c,String actor){var role=campaigns.role(c,actor);if(role==null)throw new DomainException("FORBIDDEN",403,"Membership required");return role;}
    private Image readable(UUID c,UUID id,String actor,Role role){var image=store.image(c,id);if(image==null||image.visibility().equals("DM")&&role!=Role.DM||!store.readableImage(c,id,actor,role==Role.DM))throw DiceService.missing();return image;}
    public AssetService.Asset metadata(UUID c,UUID id){var image=store.image(c,id);if(image==null)throw DiceService.missing();return image.metadata();}
    public void attach(UUID c,UUID note,List<UUID> ids,String visibility,String actor){var role=member(c,actor);for(var id:ids){var image=readable(c,id,actor,role);if(!image.visibility().equals(visibility))throw CharacterSheet.invalid();if(!image.actor().equals(actor)&&(note==null||!store.referencedBy(c,id,note)))throw new DomainException("FORBIDDEN",403,"Image belongs to another note author");}}
    public AssetService.Content content(UUID c,UUID id,String actor){return campaigns.locked(c,()->{var image=readable(c,id,actor,member(c,actor));return new AssetService.Content(image.metadata().mediaType(),files.read(image.storageKey()));});}
    public AssetService.Asset upload(UUID c,String actor,UUID request,long version,String visibility,byte[] bytes,String mediaType){
        if(request==null||version<0||!Set.of("TEAM","DM").contains(visibility))throw CharacterSheet.invalid();Image[] published={null};
        try{return campaigns.actorLocked(actor,()->campaigns.locked(c,()->{
            var role=member(c,actor);if(visibility.equals("DM"))AccountPolicy.dm(role);var decoded=AssetImage.validate(bytes,mediaType);var fp=DiceService.fingerprint(json,List.of(c,version,visibility,decoded.mediaType(),decoded.sha256()));var old=store.uploadReceipt(actor,request);
            if(old!=null){if(!old.fingerprint().equals(fp))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID already used");return readable(c,old.result().id(),actor,role).metadata();}
            AccountPolicy.version(campaigns.campaignVersion(c),version);var metadata=new AssetService.Asset(UUID.randomUUID(),decoded.mediaType(),bytes.length,decoded.width(),decoded.height());var image=new Image(metadata,actor,visibility,UUID.randomUUID().toString(),decoded.sha256());published[0]=image;files.publish(image.storageKey(),decoded.bytes());store.saveImage(c,image);store.uploadReceipt(c,actor,request,new NotesService.Receipt(fp,new NotesService.Ack(metadata.id(),0)));return metadata;
        }));}catch(RuntimeException|Error failure){if(published[0]!=null)try{boolean committed=campaigns.locked(c,()->{var saved=store.image(c,published[0].metadata().id());return saved!=null&&saved.storageKey().equals(published[0].storageKey());});if(!committed)files.discard(published[0].storageKey());}catch(RuntimeException|Error cleanup){failure.addSuppressed(cleanup);}throw failure;}
    }
}
