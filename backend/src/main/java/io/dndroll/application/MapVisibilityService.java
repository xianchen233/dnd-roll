package io.dndroll.application;

import io.dndroll.application.AssetService.MapConfig;
import io.dndroll.domain.*;
import io.dndroll.domain.Movement.Role;
import java.util.*;
import java.util.function.Supplier;

public final class MapVisibilityService {
    public record Cell(Integer x,Integer y) {}
    public record Scene(UUID id,int width,int height,int gridSize,long version,MapConfig map,List<Cell> revealed) {}
    public record PublicMap(double distancePerCell) {}
    public record View(UUID sceneId,long sceneVersion,PublicMap map) {}
    public record Fog(UUID sceneId,long sceneVersion,List<Cell> revealed) {}
    public record Intent(UUID campaignId,UUID sceneId,long expectedVersion,List<Cell> revealed) {}
    public record Receipt(Intent intent,Fog result) {}
    public interface Store {
        <T> T actorLocked(String actor,Supplier<T> work);
        <T> T locked(UUID campaign,Supplier<T> work);
        Role role(UUID campaign,String actor);
        Scene current(UUID campaign);
        Scene scene(UUID campaign,UUID scene);
        byte[] original(UUID campaign,UUID asset);
        Receipt receipt(String actor,UUID request);
        void receipt(String actor,UUID request,Receipt receipt);
        long replace(UUID campaign,UUID scene,List<Cell> cells);
    }
    private final Store store;
    private final SceneService.Events events;
    public MapVisibilityService(Store store,SceneService.Events events) {this.store=store;this.events=events;}
    public View view(UUID campaign,String actor) {
        return store.locked(campaign,()->{member(campaign,actor);var scene=current(campaign);return new View(scene.id(),scene.version(),scene.map()==null?null:new PublicMap(scene.map().distancePerCell()));});
    }
    /** Parameter validation must not disclose route state to a revoked member. The image read rechecks under its rendering lock. */
    public void authorizeImageRequest(UUID campaign,String actor) {store.locked(campaign,()->member(campaign,actor));}
    public byte[] image(UUID campaign,String actor,UUID sceneId,long version) {
        return store.locked(campaign,()->{
            Role role=member(campaign,actor);var scene=current(campaign);
            if(!scene.id().equals(sceneId))throw missing();
            AccountPolicy.version(scene.version(),version);
            var map=scene.map();if(map==null)throw missing();
            var cells=new HashSet<>(scene.revealed());
            return SafeMapImage.render(store.original(campaign,map.assetId()),scene.width(),scene.height(),scene.gridSize(),map.gridSize(),map.offsetX(),map.offsetY(),
                (x,y)->role==Role.DM||cells.contains(new Cell(x,y)));
        });
    }
    public Fog fog(UUID campaign,UUID sceneId,String actor) {
        return store.locked(campaign,()->{AccountPolicy.dm(store.role(campaign,actor));var scene=requireScene(campaign,sceneId);return new Fog(scene.id(),scene.version(),scene.revealed());});
    }
    public Fog configure(UUID campaign,UUID sceneId,String actor,UUID request,long version,List<Cell> input) {
        if(request==null||version<0||input==null)throw invalid();
        var cells=new TreeSet<Cell>(Comparator.comparingInt((Cell c)->c.y()).thenComparingInt(c->c.x()));
        for(var cell:input)if(cell==null||cell.x()==null||cell.y()==null||cell.x()<0||cell.y()<0||!cells.add(cell))throw invalid();
        var canonical=List.copyOf(cells);var intent=new Intent(campaign,sceneId,version,canonical);
        return store.actorLocked(actor,()->store.locked(campaign,()->{
            AccountPolicy.dm(store.role(campaign,actor));var scene=requireScene(campaign,sceneId);
            var previous=store.receipt(actor,request);
            if(previous!=null){if(!previous.intent().equals(intent))throw new DomainException("IDEMPOTENCY_CONFLICT",409,"Request ID was already used with different parameters");return previous.result();}
            if(canonical.size()>(long)scene.width()*scene.height()||canonical.stream().anyMatch(c->c.x()>=scene.width()||c.y()>=scene.height()))throw invalid();
            AccountPolicy.version(scene.version(),version);
            var result=new Fog(sceneId,store.replace(campaign,sceneId,canonical),canonical);
            store.receipt(actor,request,new Receipt(intent,result));events.changed(campaign);return result;
        }));
    }
    private Role member(UUID campaign,String actor){var role=store.role(campaign,actor);if(role==null)throw new DomainException("FORBIDDEN",403,"Campaign membership required");return role;}
    private Scene current(UUID campaign){var scene=store.current(campaign);if(scene==null)throw missing();return scene;}
    private Scene requireScene(UUID campaign,UUID id){var scene=store.scene(campaign,id);if(scene==null)throw missing();return scene;}
    private static DomainException missing(){return new DomainException("NOT_FOUND",404,"Map or scene not found");}
    private static DomainException invalid(){return new DomainException("INVALID_INPUT",400,"Invalid map visibility command");}
}
