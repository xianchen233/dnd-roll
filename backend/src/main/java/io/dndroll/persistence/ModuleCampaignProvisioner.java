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

    /** Ordered JSON rows bind even edits which do not advance the instance version. */
    public Object runtime(UUID c){
        var snapshot=new LinkedHashMap<String,Object>();
        for(String table:List.of("campaign","scene","scene_map","scene_revealed_cell","token","token_controller","character_card","character_controller","campaign_combat","campaign_initiative")){
            String where=table.equals("campaign")?"id":"campaign_id";
            snapshot.put(table,jdbc.queryForList("select row_to_json(r)::text from (select * from "+table+" where "+where+"=? order by to_jsonb("+table+")::text) r",String.class,c));
        }
        return snapshot;
    }
    private Map<String,Map<String,UUID>> mapping(ModuleLifecycle.Instance before){var result=new LinkedHashMap<String,Map<String,UUID>>();before.mapping().forEach((k,v)->result.put(k,new LinkedHashMap<>(v)));return result;}
    public List<String> migrationBlockers(UUID c,ModuleLifecycle.Instance before,ModuleBlueprint.Document next){
        var blockers=new ArrayList<String>();var mapped=before.mapping();
        for(var scene:next.play().scenes()){
            UUID id=mapped.get("scenes").get(scene.nodeId());if(id==null)continue;
            if(Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from token where campaign_id=? and scene_id=? and not module_retired and (x>=? or y>=?))",Boolean.class,c,id,scene.width(),scene.height())))blockers.add("/scene-bounds/"+scene.nodeId());
        }
        // Recovery must cover all active play objects; do not silently omit ad-hoc additions.
        var sceneIds=mapped.get("scenes").values();for(UUID id:jdbc.queryForList("select id from scene where campaign_id=? and not module_retired",UUID.class,c))if(!sceneIds.contains(id))blockers.add("/unmapped-scene/"+id);
        for(UUID id:jdbc.queryForList("select id from token where campaign_id=? and not module_retired",UUID.class,c))if(!mapped.get("placements").containsValue(id))blockers.add("/unmapped-placement/"+id);
        for(var card:cards.cards(c))if(!mapped.get("units").containsValue(card.id())&&!mapped.getOrDefault("retiredUnits",Map.of()).containsValue(card.id()))blockers.add("/unmapped-unit/"+card.id());
        for(var entry:mapped.get("scenes").entrySet()){
            var map=assets.map(c,entry.getValue());if(map==null)blockers.add("/missing-scene/"+entry.getKey());
            else if(map.map()!=null&&!mapped.get("assets").containsValue(map.map().assetId()))blockers.add("/unmapped-map/"+entry.getKey());
        }
        for(var p:before.document().play().placements()){
            UUID token=mapped.get("placements").get(p.id());var rows=jdbc.queryForList("select character_id,scene_id from token where campaign_id=? and id=?",c,token);
            if(rows.isEmpty()||!Objects.equals(rows.getFirst().get("character_id"),mapped.get("units").get(p.unitId()))||!Objects.equals(rows.getFirst().get("scene_id"),mapped.get("scenes").get(p.sceneId())))blockers.add("/changed-placement-link/"+p.id());
        }
        return blockers;
    }
    public ModuleBlueprint.Document recovery(UUID c,ModuleLifecycle.Instance before){
        var m=before.mapping();var nodes=new ArrayList<ModuleArchive.Node>();
        for(var node:before.document().nodes()){
            String name=node.name();if(m.get("scenes").containsKey(node.id()))name=jdbc.queryForObject("select name from scene where campaign_id=? and id=?",String.class,c,m.get("scenes").get(node.id()));
            nodes.add(new ModuleArchive.Node(node.id(),node.kind(),name,node.text(),node.visibility(),node.nodeIds(),node.assetIds(),node.dependencyIds()));
        }
        var scenes=new ArrayList<ModuleBlueprint.Scene>();
        for(var old:before.document().play().scenes()){
            UUID id=m.get("scenes").get(old.nodeId());var row=jdbc.queryForMap("select width,height,grid_size from scene where campaign_id=? and id=?",c,id);var map=assets.map(c,id).map();ModuleBlueprint.MapImage image=null;
            if(map!=null){String asset=m.get("assets").entrySet().stream().filter(e->e.getValue().equals(map.assetId())).findFirst().orElseThrow().getKey();image=new ModuleBlueprint.MapImage(asset,map.gridSize(),map.offsetX(),map.offsetY(),map.distancePerCell());}
            scenes.add(new ModuleBlueprint.Scene(old.nodeId(),((Number)row.get("width")).intValue(),((Number)row.get("height")).intValue(),((Number)row.get("grid_size")).intValue(),image));
        }
        var units=before.document().play().units().stream().map(u->{var card=cards.card(c,m.get("units").get(u.nodeId()));return new ModuleBlueprint.Unit(u.nodeId(),card.sheet(),card.hidden(),card.shareFull());}).toList();
        var placements=before.document().play().placements().stream().map(p->{var row=jdbc.queryForMap("select x,y,hidden from token where campaign_id=? and id=?",c,m.get("placements").get(p.id()));return new ModuleBlueprint.Placement(p.id(),p.sceneId(),p.unitId(),((Number)row.get("x")).intValue(),((Number)row.get("y")).intValue(),(Boolean)row.get("hidden"));}).toList();
        var play=before.document().play();return new ModuleBlueprint.Document(nodes,new ModuleBlueprint.Play(scenes,units,placements,play.admissionMode(),play.approvedDependencyIds()));
    }
    private CharacterSheet preservePlay(CharacterSheet incoming,CharacterSheet live){
        return new CharacterSheet(incoming.name(),incoming.avatar(),incoming.species(),incoming.background(),incoming.classes(),incoming.abilities(),incoming.skills(),incoming.saves(),live.combat(),incoming.attacks(),incoming.casting(),incoming.spells(),incoming.features(),live.resources(),live.equipment(),live.money(),live.conditions(),incoming.modifiers(),live.overrides(),incoming.notes());
    }
    public Map<String,Map<String,UUID>> migrate(UUID c,String actor,ModuleLifecycle.Instance before,ModuleBlueprint.Document next){
        var m=mapping(before);var nodes=new HashMap<String,ModuleArchive.Node>();next.nodes().forEach(n->nodes.put(n.id(),n));
        for(var old:before.document().play().scenes())if(next.play().scenes().stream().noneMatch(s->s.nodeId().equals(old.nodeId()))){UUID id=m.get("scenes").remove(old.nodeId());jdbc.update("update scene set module_retired=true,version=version+1 where campaign_id=? and id=?",c,id);}
        for(var s:next.play().scenes()){
            UUID id=m.get("scenes").get(s.nodeId());var old=before.document().play().scenes().stream().filter(v->v.nodeId().equals(s.nodeId())).findFirst().orElse(null);
            if(id==null){id=UUID.randomUUID();m.get("scenes").put(s.nodeId(),id);jdbc.update("insert into scene(id,campaign_id,name,width,height,grid_size) values (?,?,?,?,?,?)",id,c,nodes.get(s.nodeId()).name(),s.width(),s.height(),s.gridSize());}
            else if(!s.equals(old)||!Objects.equals(nodes.get(s.nodeId()).name(),before.document().nodes().stream().filter(n->n.id().equals(s.nodeId())).findFirst().orElseThrow().name()))jdbc.update("update scene set name=?,width=?,height=?,grid_size=?,version=version+1 where campaign_id=? and id=?",nodes.get(s.nodeId()).name(),s.width(),s.height(),s.gridSize(),c,id);
            if(old==null||!Objects.equals(s.map(),old.map())){var a=s.map();assets.setMap(c,id,a==null?null:new AssetService.MapConfig(m.get("assets").get(a.assetId()),a.gridSize(),a.offsetX(),a.offsetY(),a.distancePerCell()));}
        }
        UUID current=jdbc.queryForObject("select current_scene_id from campaign where id=?",UUID.class,c);
        if(Boolean.TRUE.equals(jdbc.queryForObject("select module_retired from scene where id=?",Boolean.class,current))){
            UUID replacement=m.get("scenes").values().stream().findFirst().orElse(null);if(replacement==null){replacement=UUID.randomUUID();jdbc.update("insert into scene(id,campaign_id,name,width,height,grid_size) values (?,?,'迁移后场景',24,16,48)",replacement,c);}
            jdbc.update("update campaign set current_scene_id=? where id=?",replacement,c);
        }
        for(var old:before.document().play().units())if(next.play().units().stream().noneMatch(u->u.nodeId().equals(old.nodeId()))){UUID retired=m.get("units").remove(old.nodeId());m.computeIfAbsent("retiredUnits",k->new LinkedHashMap<>()).put(retired.toString(),retired);}
        for(var u:next.play().units()){
            UUID id=m.get("units").get(u.nodeId());var old=before.document().play().units().stream().filter(v->v.nodeId().equals(u.nodeId())).findFirst().orElse(null);if(u.equals(old))continue;
            var card=id==null?null:cards.card(c,id);if(id==null){id=UUID.randomUUID();m.get("units").put(u.nodeId(),id);}
            var sheet=card==null?u.sheet():preservePlay(u.sheet(),card.sheet());sheet.validate();
            var updated=new CharacterService.Card(id,c,card==null?0:card.version()+1,u.hidden(),u.shareFull(),card!=null&&card.importReview(),card==null?List.of(actor):card.controllers(),sheet);
            cards.save(updated,card==null);cards.history(new CharacterService.History(updated.version(),actor,"MODULE_MIGRATE","确认模组结构迁移；保留进行中资源",clock.instant(),card,updated));
        }
        for(var old:before.document().play().placements())if(next.play().placements().stream().noneMatch(p->p.id().equals(old.id()))){UUID id=m.get("placements").remove(old.id());jdbc.update("update token set module_retired=true,version=version+1 where campaign_id=? and id=?",c,id);}
        for(var p:next.play().placements()){
            UUID id=m.get("placements").get(p.id());var old=before.document().play().placements().stream().filter(v->v.id().equals(p.id())).findFirst().orElse(null);
            if(p.equals(old)){
                var oldUnit=before.document().play().units().stream().filter(u->u.nodeId().equals(p.unitId())).findFirst().orElseThrow();var newUnit=next.play().units().stream().filter(u->u.nodeId().equals(p.unitId())).findFirst().orElseThrow();
                if(!oldUnit.hidden()&&newUnit.hidden())jdbc.update("update token set hidden=true,version=version+1 where campaign_id=? and id=?",c,id);
                // A definition becoming public never reveals a separately hidden live token.
                continue;
            }
            var unit=next.play().units().stream().filter(u->u.nodeId().equals(p.unitId())).findFirst().orElseThrow();boolean hidden=p.hidden()||unit.hidden();
            if(id==null){id=UUID.randomUUID();m.get("placements").put(p.id(),id);jdbc.update("insert into token(id,scene_id,campaign_id,name,x,y,hidden,character_id) values (?,?,?,?,?,?,?,?)",id,m.get("scenes").get(p.sceneId()),c,unit.sheet().name(),p.x(),p.y(),hidden,m.get("units").get(p.unitId()));jdbc.update("insert into token_controller(token_id,campaign_id,username) values (?,?,?)",id,c,actor);}
            else {
                jdbc.update("update token set scene_id=?,character_id=?,x=?,y=?,hidden=?,version=version+1 where campaign_id=? and id=?",m.get("scenes").get(p.sceneId()),m.get("units").get(p.unitId()),p.x(),p.y(),hidden,c,id);
                if(!p.unitId().equals(old.unitId())){jdbc.update("delete from token_controller where campaign_id=? and token_id=?",c,id);jdbc.update("insert into token_controller(token_id,campaign_id,username) values (?,?,?)",id,c,actor);}
            }
        }
        jdbc.update("update campaign set version=version+1 where id=?",c);return m;
    }
}
