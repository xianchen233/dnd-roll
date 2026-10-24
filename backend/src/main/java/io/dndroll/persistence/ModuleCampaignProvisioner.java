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
        for(String table:List.of("campaign","scene","scene_map","scene_revealed_cell","campaign_asset","token","token_controller","character_card","character_controller","campaign_combat","campaign_initiative")){
            String where=table.equals("campaign")?"id":"campaign_id";
            snapshot.put(table,jdbc.queryForList("select row_to_json(r)::text from (select * from "+table+" where "+where+"=? order by to_jsonb("+table+")::text) r",String.class,c));
        }
        return snapshot;
    }
    private Map<String,Map<String,UUID>> mapping(ModuleLifecycle.Instance before){var result=new LinkedHashMap<String,Map<String,UUID>>();before.mapping().forEach((k,v)->result.put(k,new LinkedHashMap<>(v)));return result;}
    private record PlacementState(int x,int y,boolean hidden) {}
    /** Preview and apply use the same field-wise merge against the locked live token. */
    private PlacementState placementState(UUID c,ModuleLifecycle.Instance before,ModuleBlueprint.Placement next,boolean unitHidden){
        var old=before.document().play().placements().stream().filter(p->p.id().equals(next.id())).findFirst().orElse(null);
        if(old==null)return new PlacementState(next.x(),next.y(),next.hidden()||unitHidden);
        var rows=jdbc.queryForList("select x,y,hidden from token where campaign_id=? and id=?",c,before.mapping().get("placements").get(next.id()));
        if(rows.isEmpty())return new PlacementState(next.x(),next.y(),next.hidden()||unitHidden); // Missing link is an explicit blocker.
        var live=rows.getFirst();boolean newScene=!next.sceneId().equals(old.sceneId());
        int x=newScene||next.x()!=old.x()?next.x():((Number)live.get("x")).intValue();
        int y=newScene||next.y()!=old.y()?next.y():((Number)live.get("y")).intValue();
        return new PlacementState(x,y,(Boolean)live.get("hidden")||next.hidden()||unitHidden);
    }
    public List<String> migrationBlockers(UUID c,ModuleLifecycle.Instance before,ModuleBlueprint.Document next){
        var blockers=new ArrayList<String>();var mapped=before.mapping();
        for(var scene:next.play().scenes()){
            for(var p:next.play().placements())if(p.sceneId().equals(scene.nodeId())){
                var state=placementState(c,before,p,false);
                if(state.x()<0||state.y()<0||state.x()>=scene.width()||state.y()>=scene.height()){
                    blockers.add("/scene-bounds/"+scene.nodeId());break;
                }
            }
            UUID runtime=mapped.get("scenes").get(scene.nodeId());
            if(runtime!=null)for(var row:jdbc.queryForList("select id,x,y from token where campaign_id=? and scene_id=? and not module_retired",c,runtime)){
                if(before.document().play().placements().stream().anyMatch(p->Objects.equals(mapped.get("placements").get(p.id()),row.get("id"))))continue;
                if(((Number)row.get("x")).intValue()>=scene.width()||((Number)row.get("y")).intValue()>=scene.height()){blockers.add("/scene-bounds/"+scene.nodeId());break;}
            }
        }
        // Ad-hoc objects are recovered independently, never adopted into this mapping.
        for(var entry:mapped.get("scenes").entrySet()){
            if(!Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from scene where campaign_id=? and id=? and not module_retired)",Boolean.class,c,entry.getValue())))blockers.add("/missing-scene/"+entry.getKey());
        }
        for(var entry:mapped.get("units").entrySet())if(cards.card(c,entry.getValue())==null)blockers.add("/missing-unit/"+entry.getKey());
        for(var p:before.document().play().placements()){
            UUID token=mapped.get("placements").get(p.id());var rows=jdbc.queryForList("select character_id,scene_id from token where campaign_id=? and id=? and not module_retired",c,token);
            if(rows.isEmpty()||!Objects.equals(rows.getFirst().get("character_id"),mapped.get("units").get(p.unitId()))||!Objects.equals(rows.getFirst().get("scene_id"),mapped.get("scenes").get(p.sceneId())))blockers.add("/changed-placement-link/"+p.id());
        }
        return blockers;
    }
    private static String recoveryId(UUID runtime,Set<String> used){
        String id=runtime.toString();int suffix=0;
        while(!used.add(id))id=UUID.nameUUIDFromBytes(("recovery:"+runtime+":"+(++suffix)).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();return id;
    }
    private static Map<UUID,String> reverse(Map<String,UUID> mapping){var result=new LinkedHashMap<UUID,String>();mapping.forEach((k,v)->result.put(v,k));return result;}
    public ModuleLifecycle.Recovery recovery(UUID c,ModuleLifecycle.Instance before,ModuleArchive.Verified source,Map<String,byte[]> sourceImages){
        var m=before.mapping();var nodes=new LinkedHashMap<String,ModuleArchive.Node>();before.document().nodes().forEach(n->nodes.put(n.id(),n));
        var used=new HashSet<>(nodes.keySet());used.addAll(m.get("placements").keySet());
        var sceneIds=reverse(m.get("scenes"));var unitIds=reverse(m.get("units"));var placementIds=reverse(m.get("placements"));var assetIds=reverse(m.get("assets"));
        var confirmations=new ArrayList<String>();var scenes=new ArrayList<ModuleBlueprint.Scene>();var units=new ArrayList<ModuleBlueprint.Unit>();var placements=new ArrayList<ModuleBlueprint.Placement>();var tokens=new ArrayList<ModuleLifecycle.RecoveryToken>();
        var recoveredImages=new LinkedHashMap<>(sourceImages);var recoveredAssets=new LinkedHashMap<String,ModuleArchive.Asset>();source.manifest().assets().forEach(a->recoveredAssets.put(a.id(),a));var usedAssets=new HashSet<>(recoveredAssets.keySet());
        // Include active ad-hoc scenes, including the scene created when the last mapped scene retires.
        for(var row:jdbc.queryForList("select * from scene s where campaign_id=? and (not module_retired or exists(select 1 from token t where t.scene_id=s.id and not t.module_retired)) order by id",c)){
            UUID runtime=(UUID)row.get("id");String id=sceneIds.get(runtime);boolean extra=id==null;if(extra){id=recoveryId(runtime,used);sceneIds.put(runtime,id);confirmations.add("RECOVER_EXTRA_SCENE "+id+"; independent scene; original remains unmanaged");}
            var old=nodes.get(id);nodes.put(id,new ModuleArchive.Node(id,"SCENE",(String)row.get("name"),old==null?"":old.text(),old==null?"DM":old.visibility(),old==null?List.of():old.nodeIds(),old==null?List.of():old.assetIds(),old==null?List.of():old.dependencyIds()));
            var map=assets.map(c,runtime).map();ModuleBlueprint.MapImage image=null;
            if(map!=null){
                String asset=assetIds.get(map.assetId());if(asset==null){asset=recoveryId(map.assetId(),usedAssets);assetIds.put(map.assetId(),asset);confirmations.add("RECOVER_EXTRA_MAP "+asset+"; independent validated bytes; recovery fully covered");}
                var stored=assets.asset(c,map.assetId());if(stored==null)throw ModuleBlueprint.invalid();
                if(recoveredAssets.size()+(recoveredAssets.containsKey(asset)?0:1)>128||recoveredAssets.values().stream().filter(a->!a.id().equals(assetIds.get(map.assetId()))).mapToLong(ModuleArchive.Asset::size).sum()+stored.asset().byteSize()>ModuleArchive.EXPANDED_LIMIT)throw ModuleBlueprint.invalid();
                byte[] bytes=files.read(stored.storageKey());var valid=AssetImage.validate(bytes,stored.asset().mediaType());
                if(!valid.sha256().equals(stored.sha256())||bytes.length!=stored.asset().byteSize()||valid.width()!=stored.asset().width()||valid.height()!=stored.asset().height())throw ModuleBlueprint.invalid();
                recoveredImages.put(asset,bytes);recoveredAssets.put(asset,new ModuleArchive.Asset(asset,valid.mediaType(),bytes.length,valid.sha256()));image=new ModuleBlueprint.MapImage(asset,map.gridSize(),map.offsetX(),map.offsetY(),map.distancePerCell());
            }
            scenes.add(new ModuleBlueprint.Scene(id,((Number)row.get("width")).intValue(),((Number)row.get("height")).intValue(),((Number)row.get("grid_size")).intValue(),image));
        }
        var liveCards=new ArrayList<CharacterService.Card>();for(var u:before.document().play().units())liveCards.add(cards.card(c,m.get("units").get(u.nodeId())));
        for(var card:cards.cards(c))if(!m.get("units").containsValue(card.id()))liveCards.add(card);
        for(var card:liveCards){
            String id=unitIds.get(card.id());if(id==null){id=recoveryId(card.id(),used);unitIds.put(card.id(),id);nodes.put(id,new ModuleArchive.Node(id,"UNIT",card.sheet().name(),"","DM",List.of(),List.of(),List.of()));confirmations.add("RECOVER_EXTRA_UNIT "+id+"; independent card with DM-only control; no combat or history replay");}
            units.add(new ModuleBlueprint.Unit(id,card.sheet(),card.hidden(),card.shareFull()));
        }
        for(var row:jdbc.queryForList("select * from token where campaign_id=? and not module_retired order by id",c)){
            UUID runtime=(UUID)row.get("id");String id=placementIds.get(runtime);if(id==null){id=recoveryId(runtime,used);confirmations.add("RECOVER_EXTRA_PLACEMENT "+id+"; independent token with DM-only control; original remains unmanaged");}
            String scene=sceneIds.get((UUID)row.get("scene_id")),unit=unitIds.get((UUID)row.get("character_id"));if(scene==null||row.get("character_id")!=null&&unit==null)throw ModuleBlueprint.invalid();
            int x=((Number)row.get("x")).intValue(),y=((Number)row.get("y")).intValue();boolean hidden=(Boolean)row.get("hidden");var geometry=scenes.stream().filter(s->s.nodeId().equals(scene)).findFirst().orElseThrow();
            if(x<0||y<0||x>=geometry.width()||y>=geometry.height())throw ModuleBlueprint.invalid();
            if(unit!=null)placements.add(new ModuleBlueprint.Placement(id,scene,unit,x,y,hidden));
            tokens.add(new ModuleLifecycle.RecoveryToken(id,scene,(String)row.get("name"),x,y,hidden,unit==null));
        }
        if(tokens.size()>500)throw ModuleBlueprint.invalid();
        var play=before.document().play();var document=new ModuleBlueprint.Document(new ArrayList<>(nodes.values()),new ModuleBlueprint.Play(scenes,units,placements,play.admissionMode(),play.approvedDependencyIds()));var s=source.manifest();
        var manifest=new ModuleArchive.Manifest(s.format(),2,s.moduleId(),s.moduleVersion(),s.name(),s.rules(),s.rights(),s.dependencies(),s.content(),new ArrayList<>(recoveredAssets.values()));
        var archive=new ModuleArchive();var verified=archive.verify(archive.export(manifest,document,recoveredImages,s.moduleId(),s.moduleVersion(),s.name()));
        UUID current=jdbc.queryForObject("select current_scene_id from campaign where id=?",UUID.class,c);String currentScene=sceneIds.get(current);if(currentScene==null)throw ModuleBlueprint.invalid();
        return new ModuleLifecycle.Recovery(document,verified,recoveredImages,currentScene,tokens,confirmations);
    }
    public Map<String,Map<String,UUID>> createRecovery(UUID source,UUID target,String actor,ModuleLifecycle.Recovery recovery){
        var mapping=create(source,target,actor,"迁移前恢复副本",recovery.verified(),recovery.document(),recovery.images());
        for(var token:recovery.tokens()){
            UUID id=mapping.get("placements").get(token.id());
            if(token.bare()){id=UUID.randomUUID();mapping.get("placements").put(token.id(),id);jdbc.update("insert into token(id,scene_id,campaign_id,name,x,y,hidden) values (?,?,?,?,?,?,?)",id,mapping.get("scenes").get(token.sceneId()),target,token.name(),token.x(),token.y(),token.hidden());jdbc.update("insert into token_controller(token_id,campaign_id,username) values (?,?,?)",id,target,actor);}
            else jdbc.update("update token set name=?,hidden=? where campaign_id=? and id=?",token.name(),token.hidden(),target,id);
        }
        jdbc.update("update campaign set current_scene_id=? where id=?",mapping.get("scenes").get(recovery.currentScene()),target);return mapping;
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
                var state=placementState(c,before,p,unit.hidden());
                jdbc.update("update token set scene_id=?,character_id=?,x=?,y=?,hidden=?,version=version+1 where campaign_id=? and id=?",m.get("scenes").get(p.sceneId()),m.get("units").get(p.unitId()),state.x(),state.y(),state.hidden(),c,id);
                if(!p.unitId().equals(old.unitId())){jdbc.update("delete from token_controller where campaign_id=? and token_id=?",c,id);jdbc.update("insert into token_controller(token_id,campaign_id,username) values (?,?,?)",id,c,actor);}
            }
        }
        jdbc.update("update campaign set version=version+1 where id=?",c);return m;
    }
}
