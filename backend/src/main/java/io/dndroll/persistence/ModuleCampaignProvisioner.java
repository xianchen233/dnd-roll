package io.dndroll.persistence;

import io.dndroll.modules.*;
import io.dndroll.application.*;
import io.dndroll.domain.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.*;

/** Persists a validated definition into the existing play model in the caller's transaction. */
@Component
public class ModuleCampaignProvisioner implements ModuleLifecycle.Provisioner {
    private final JdbcTemplate jdbc;private final CharacterService.Store cards;private final AssetService.Store assets;private final AssetService.Files files;private final NotesService.Store notes;private final NoteImages.Store images;private final ContentService.Store content;private final Clock clock;private final TransactionTemplate reconcile;
    public ModuleCampaignProvisioner(JdbcTemplate jdbc,CharacterService.Store cards,AssetService.Store assets,AssetService.Files files,NotesService.Store notes,NoteImages.Store images,ContentService.Store content,Clock clock,org.springframework.transaction.PlatformTransactionManager transactions){this.jdbc=jdbc;this.cards=cards;this.assets=assets;this.files=files;this.notes=notes;this.images=images;this.content=content;this.clock=clock;reconcile=new TransactionTemplate(transactions);reconcile.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);}
    private String publish(UUID source,byte[] bytes){
        if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Module creation requires a transaction");String key=UUID.randomUUID().toString();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){
            if(status==STATUS_ROLLED_BACK)files.discard(key);
            else if(status==STATUS_UNKNOWN)try{
                // Commit errors include deferred constraint failures and lost acknowledgments.
                // A fresh source lock waits for the original server transaction to finish.
                reconcile.execute(ignored->{jdbc.query("select id from campaign where id=? for update",(r,n)->r.getObject(1,UUID.class),source);
                    boolean saved=Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from campaign_asset where storage_key=?) or exists(select 1 from note_attachment where storage_key=?::uuid)",Boolean.class,key,key));
                    if(!saved)files.discard(key);return null;});
            }catch(RuntimeException unknown){/* Keep opaque files when the commit outcome cannot be established. */}
        }});
        files.publish(key,bytes);return key;
    }
    public Map<String,Map<String,UUID>> create(UUID source,UUID c,String actor,String name,ModuleArchive.Verified verified,ModuleBlueprint.Document document,Map<String,byte[]> bytes){
        jdbc.update("insert into campaign(id,name) values (?,?)",c,name);jdbc.update("insert into campaign_member(campaign_id,username,role) values (?,?,'DM')",c,actor);
        var mapping=new LinkedHashMap<String,Map<String,UUID>>();for(String kind:List.of("scenes","units","placements","assets","documents","dependencies","noteImages"))mapping.put(kind,new LinkedHashMap<>());
        var nodes=new HashMap<String,ModuleArchive.Node>();document.nodes().forEach(n->nodes.put(n.id(),n));
        // Dependency identities remain pinned in the instance source; mutable target entries receive fresh IDs.
        var dependencies=verified.manifest().dependencies().stream().map(ModuleArchive.Dependency::id).toList();
        for(var entry:content.entries(source))if(dependencies.contains(entry.id().toString())){UUID id=UUID.randomUUID();content.entry(c,new ContentEntry(id,entry.data(),false,entry.license(),entry.attribution(),entry.changes()));mapping.get("dependencies").put(entry.id().toString(),id);}
        // Builtins are always admitted by P04. Only copied custom entries belong
        // in its approval list, otherwise saving that returned policy would fail.
        var approved=document.play().approvedDependencyIds().stream().filter(mapping.get("dependencies")::containsKey).map(mapping.get("dependencies")::get).toList();
        for(String id:dependencies)mapping.get("dependencies").putIfAbsent(id,UUID.fromString(id));
        content.policy(c,new ContentService.Policy(document.play().admissionMode(),approved));
        for(var a:verified.manifest().assets()){
            var image=AssetImage.validate(bytes.get(a.id()),a.mediaType());UUID id=UUID.randomUUID();String key=publish(source,image.bytes());var meta=new AssetService.Asset(id,image.mediaType(),image.bytes().length,image.width(),image.height());assets.insertAsset(c,new AssetService.Stored(meta,key,image.sha256()));mapping.get("assets").put(a.id(),id);
        }
        for(var s:document.play().scenes()){
            UUID id=UUID.randomUUID();mapping.get("scenes").put(s.nodeId(),id);jdbc.update("insert into scene(id,campaign_id,name,width,height,grid_size) values (?,?,?,?,?,?)",id,c,nodes.get(s.nodeId()).name(),s.width(),s.height(),s.gridSize());
            if(s.map()!=null){var m=s.map();assets.setMap(c,id,new AssetService.MapConfig(mapping.get("assets").get(m.assetId()),m.gridSize(),m.offsetX(),m.offsetY(),m.distancePerCell()));}
        }
        UUID current;if(mapping.get("scenes").isEmpty()){current=UUID.randomUUID();jdbc.update("insert into scene(id,campaign_id,name,width,height,grid_size) values (?,?,'First scene',24,16,48)",current,c);}else current=mapping.get("scenes").values().iterator().next();jdbc.update("update campaign set current_scene_id=? where id=?",current,c);
        for(var u:document.play().units()){
            UUID id=UUID.randomUUID();mapping.get("units").put(u.nodeId(),id);var card=new CharacterService.Card(id,c,0,u.hidden(),u.shareFull(),false,List.of(actor),u.sheet());cards.save(card,true);cards.history(new CharacterService.History(0,actor,"MODULE_CREATE","从定制副本创建独立角色",clock.instant(),null,card));
        }
        for(var p:document.play().placements()){
            UUID id=UUID.randomUUID();mapping.get("placements").put(p.id(),id);var unit=document.play().units().stream().filter(u->u.nodeId().equals(p.unitId())).findFirst().orElseThrow();
            jdbc.update("insert into token(id,scene_id,campaign_id,name,x,y,hidden,character_id) values (?,?,?,?,?,?,?,?)",id,mapping.get("scenes").get(p.sceneId()),c,unit.sheet().name(),p.x(),p.y(),p.hidden()||unit.hidden(),mapping.get("units").get(p.unitId()));jdbc.update("insert into token_controller(token_id,campaign_id,username) values (?,?,?)",id,c,actor);
        }
        for(var n:document.nodes())if(n.kind().equals("DOCUMENT")){
            String visibility=n.visibility().equals("PUBLIC")?"TEAM":"DM";var attachments=new ArrayList<UUID>();
            for(String asset:n.assetIds()){
                String identity=asset+":"+visibility;UUID imageId=mapping.get("noteImages").get(identity);
                if(imageId==null){var a=verified.manifest().assets().stream().filter(v->v.id().equals(asset)).findFirst().orElseThrow();var image=AssetImage.validate(bytes.get(asset),a.mediaType());imageId=UUID.randomUUID();var meta=new AssetService.Asset(imageId,image.mediaType(),image.bytes().length,image.width(),image.height());images.saveImage(c,new NoteImages.Image(meta,actor,visibility,publish(source,image.bytes()),image.sha256()));mapping.get("noteImages").put(identity,imageId);}attachments.add(imageId);
            }
            UUID id=UUID.randomUUID();mapping.get("documents").put(n.id(),id);var now=clock.instant();notes.save(c,new NotesService.Note(id,0,visibility,n.name(),n.text(),List.copyOf(attachments),actor,now,now,"从定制副本创建资料"),true);
        }
        return mapping;
    }
}
