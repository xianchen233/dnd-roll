package io.dndroll.application;

import io.dndroll.domain.*;
import io.dndroll.domain.Movement.Role;
import java.util.*;

/** Images referenced by immutable RP revisions, separate from DM preparation assets. */
public final class TimelineImages {
    public record Image(AssetService.Asset metadata,String actor,String storageKey,String sha256) {}
    public interface Store {
        Image image(UUID c,UUID id);void saveImage(UUID c,Image image);
        boolean readableImage(UUID c,UUID id,String actor,boolean dm);
        TimelineService.Receipt receipt(String ns,String actor,UUID id);
        void receipt(String ns,UUID c,String actor,UUID id,TimelineService.Receipt r);
    }
    private final Store store;private final CharacterService.Store cards;private final AssetService.Files files;
    public TimelineImages(Store store,CharacterService.Store cards,AssetService.Files files){this.store=store;this.cards=cards;this.files=files;}
    private Role member(UUID c,String actor){var r=cards.role(c,actor);if(r==null)throw new DomainException("FORBIDDEN",403,"Membership required");return r;}
    private Image readable(UUID c,UUID id,String actor,Role role){var i=store.image(c,id);if(i==null||!store.readableImage(c,id,actor,role==Role.DM))throw DiceService.missing();return i;}
    public AssetService.Asset metadata(UUID c,UUID id){var i=store.image(c,id);if(i==null)throw DiceService.missing();return i.metadata();}
    public void attach(UUID c,List<UUID> ids,String actor){var role=member(c,actor);for(var id:ids){var i=readable(c,id,actor,role);if(!i.actor().equals(actor))throw new DomainException("FORBIDDEN",403,"Only uploader may attach image");}}
    public AssetService.Content content(UUID c,UUID id,String actor){return cards.locked(c,()->{var i=readable(c,id,actor,member(c,actor));return new AssetService.Content(i.metadata().mediaType(),files.read(i.storageKey()));});}
    public AssetService.Asset upload(UUID c,String actor,UUID request,long version,byte[] bytes,String mediaType){
        if(request==null||version<0)throw CharacterSheet.invalid();Image[] published={null};
        try{return cards.actorLocked(actor,()->cards.locked(c,()->{
            var role=member(c,actor);var image=AssetImage.validate(bytes,mediaType);String fp=c+"|"+version+"|"+image.mediaType()+"|"+image.sha256();var old=store.receipt("attachments",actor,request);
            if(old!=null){if(!old.fingerprint().equals(DiceService.fingerprint(tools.jackson.databind.json.JsonMapper.builder().build(),fp)))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID already used");return readable(c,old.result().id(),actor,role).metadata();}
            AccountPolicy.version(cards.campaignVersion(c),version);var metadata=new AssetService.Asset(UUID.randomUUID(),image.mediaType(),bytes.length,image.width(),image.height());var saved=new Image(metadata,actor,UUID.randomUUID().toString(),image.sha256());published[0]=saved;files.publish(saved.storageKey(),image.bytes());store.saveImage(c,saved);store.receipt("attachments",c,actor,request,new TimelineService.Receipt(DiceService.fingerprint(tools.jackson.databind.json.JsonMapper.builder().build(),fp),new TimelineService.Ack(metadata.id(),0)));return metadata;
        }));}catch(RuntimeException|Error failure){if(published[0]!=null)try{boolean committed=cards.locked(c,()->{var saved=store.image(c,published[0].metadata().id());return saved!=null&&saved.storageKey().equals(published[0].storageKey());});if(!committed)files.discard(published[0].storageKey());}catch(RuntimeException|Error cleanup){failure.addSuppressed(cleanup);}throw failure;}
    }
}
