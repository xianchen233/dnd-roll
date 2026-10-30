package io.dndroll.modules;

import io.dndroll.domain.*;
import java.lang.reflect.*;
import java.time.Instant;
import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Typed exchange definitions. No runtime identity, authorization or history fields. */
public final class ModuleBlueprint {
    public record Document(List<ModuleArchive.Node> nodes,Play play,@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) ModulePortable.Block operations) {
        public Document(List<ModuleArchive.Node> nodes,Play play){this(nodes,play,null);}
    }
    public record Play(List<Scene> scenes,List<Unit> units,List<Placement> placements,String admissionMode,List<String> approvedDependencyIds) {}
    public record Scene(String nodeId,int width,int height,int gridSize,MapImage map) {}
    public record MapImage(String assetId,int gridSize,int offsetX,int offsetY,double distancePerCell) {}
    public record Unit(String nodeId,CharacterSheet sheet,boolean hidden,boolean shareFull) {}
    public record Placement(String id,String sceneId,String unitId,int x,int y,boolean hidden) {}
    public static Play empty(){return new Play(List.of(),List.of(),List.of(),"REVIEW",List.of());}
    private static final JsonMapper JSON=JsonMapper.builder().build();
    public static Document parse(JsonNode content,ModuleArchive.Manifest manifest){
        try {
            Document document;
            if(manifest.formatVersion()==1){
                if(!content.isObject()||content.size()!=1||!content.has("nodes"))throw invalid();
                shape(content.get("nodes"),Document.class.getRecordComponents()[0].getGenericType());
                document=new Document(Arrays.asList(JSON.treeToValue(content.get("nodes"),ModuleArchive.Node[].class)),empty());
            }else{
                if(manifest.formatVersion()!=2&&manifest.formatVersion()!=3||content.has("operations")!=(manifest.formatVersion()==3))throw invalid();
                shape(content,Document.class);document=JSON.treeToValue(content,Document.class);
            }
            validate(document,manifest,manifest.formatVersion()==1);return document;
        }catch(RuntimeException e){throw invalid();}
    }
    public static void validate(Document d,ModuleArchive.Manifest manifest){
        validate(d,manifest,false);
    }
    private static void validate(Document d,ModuleArchive.Manifest manifest,boolean legacyContent){
        if(d==null||d.nodes()==null||d.play()==null)throw invalid();
        // Preserve v1's original content-byte allowance. Empty v2 play is added to
        // the editing baseline, so counting that wrapper would reject valid v1 ZIPs.
        if(JSON.writeValueAsBytes(legacyContent?Map.of("nodes",d.nodes()):d).length>ModuleArchive.JSON_LIMIT)throw invalid();
        var nodes=new LinkedHashMap<String,ModuleArchive.Node>();
        if(d.nodes().size()>500)throw invalid();
        for(var n:d.nodes()){
            id(n.id());if(nodes.put(n.id(),n)!=null||!Set.of("CHAPTER","SCENE","UNIT","ENCOUNTER","DOCUMENT","REWARD","TRIGGER").contains(n.kind())||!Set.of("DM","PUBLIC").contains(n.visibility()))throw invalid();
            CharacterSheet.text(n.name(),100,true);CharacterSheet.text(n.text(),10000,false);
        }
        var assets=new HashSet<String>();manifest.assets().forEach(a->assets.add(a.id()));var deps=new HashSet<String>();manifest.dependencies().forEach(a->deps.add(a.id()));
        for(var n:d.nodes()){refs(n.nodeIds(),nodes.keySet());refs(n.assetIds(),assets);refs(n.dependencyIds(),deps);if(!legacyContent&&n.kind().equals("DOCUMENT")&&n.assetIds().size()>4)throw invalid();}
        var p=d.play();if(p.scenes()==null||p.units()==null||p.placements()==null||p.scenes().size()>50||p.units().size()>100||p.placements().size()>500||!Set.of("REVIEW","FREE").contains(p.admissionMode()))throw invalid();refs(p.approvedDependencyIds(),deps);
        var scenes=new HashMap<String,Scene>();var units=new HashSet<String>();var placements=new HashSet<String>();
        for(var s:p.scenes()){
            kind(nodes,s.nodeId(),"SCENE");if(scenes.put(s.nodeId(),s)!=null)throw invalid();range(s.width(),1,200);range(s.height(),1,200);range(s.gridSize(),8,512);
            if(s.map()!=null){var m=s.map();if(!assets.contains(m.assetId()))throw invalid();range(m.gridSize(),8,512);range(m.offsetX(),-8192,8192);range(m.offsetY(),-8192,8192);if(!Double.isFinite(m.distancePerCell())||m.distancePerCell()<0.01||m.distancePerCell()>10000)throw invalid();}
        }
        for(var u:p.units()){kind(nodes,u.nodeId(),"UNIT");if(!units.add(u.nodeId())||u.sheet()==null)throw invalid();u.sheet().validate();}
        for(var v:p.placements()){id(v.id());if(nodes.containsKey(v.id())||!placements.add(v.id())||!scenes.containsKey(v.sceneId())||!units.contains(v.unitId()))throw invalid();var s=scenes.get(v.sceneId());range(v.x(),0,s.width()-1);range(v.y(),0,s.height()-1);}
        ModulePortable.validate(d.operations(),d);
    }
    private static void kind(Map<String,ModuleArchive.Node> nodes,String id,String kind){if(!nodes.containsKey(id)||!nodes.get(id).kind().equals(kind))throw invalid();}
    private static void refs(List<String> refs,Set<String> known){if(refs==null||refs.size()>500||new HashSet<>(refs).size()!=refs.size()||!known.containsAll(refs))throw invalid();}
    public static void id(String id){if(id==null||!UUID.fromString(id).toString().equals(id))throw invalid();}
    private static void range(int v,int min,int max){if(v<min||v>max)throw invalid();}
    public static <T>T command(JsonNode n,Class<T> type){try{shape(n,type);if(JSON.writeValueAsBytes(n).length>2*ModuleArchive.JSON_LIMIT)throw invalid();return JSON.treeToValue(n,type);}catch(RuntimeException e){throw invalid();}}
    public static void shape(JsonNode n,Type type){
        if(n==null||n.isNull())throw invalid();
        if(type instanceof ParameterizedType p){var args=p.getActualTypeArguments();if(p.getRawType()==List.class){if(!n.isArray()||n.size()>500)throw invalid();n.forEach(v->shape(v,args[0]));}else if(p.getRawType()==Map.class){if(!n.isObject()||n.size()>500)throw invalid();n.properties().forEach(e->shape(e.getValue(),args[1]));}return;}
        Class<?> c=(Class<?>)type;
        if(c==ModulePortable.Step.class){ModulePortable.shape(n);return;}
        if(c.isRecord()){
            boolean oldDocument=c==Document.class&&!n.has("operations");
            if(!n.isObject()||n.size()!=c.getRecordComponents().length-(oldDocument?1:0))throw invalid();
            for(var f:c.getRecordComponents()){
                if(oldDocument&&f.getName().equals("operations"))continue;
                var value=n.get(f.getName());if(value==null)throw invalid();
                if(value.isNull()&&((c==Scene.class&&f.getName().equals("map"))||(c==CharacterSheet.Condition.class&&Set.of("remainingRounds","expiresAt").contains(f.getName()))))continue;
                shape(value,f.getGenericType());
            }
        }else if(c==String.class||c==UUID.class||c==Instant.class){if(!n.isString())throw invalid();}
        else if(c==int.class||c==Integer.class){if(!n.isIntegralNumber()||!n.canConvertToInt())throw invalid();}
        else if(c==long.class){if(!n.isIntegralNumber()||!n.canConvertToLong())throw invalid();}
        else if(c==double.class){if(!n.isNumber()||!Double.isFinite(n.asDouble()))throw invalid();}
        else if(c==boolean.class&&!n.isBoolean())throw invalid();
    }
    public static DomainException invalid(){return new DomainException("INVALID_MODULE",400,"Invalid or incompatible module definition");}
}
