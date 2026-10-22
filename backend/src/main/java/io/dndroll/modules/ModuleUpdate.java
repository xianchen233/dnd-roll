package io.dndroll.modules;

import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Stable-identity three-way comparison. Preview has no persistence capability. */
public final class ModuleUpdate {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final JsonNode NULL=JSON.readTree("null");
    private static final Map<String,String> IDENTITIES=Map.of("/nodes","id","/play/scenes","nodeId","/play/units","nodeId","/play/placements","id");
    public record Change(String path,JsonNode baseline,JsonNode current,JsonNode incoming,boolean conflict) {}
    public static List<Change> compare(ModuleBlueprint.Document baseline,ModuleBlueprint.Document current,ModuleBlueprint.Document incoming) {
        var changes=new ArrayList<Change>();
        compare("",JSON.valueToTree(baseline),JSON.valueToTree(current),JSON.valueToTree(incoming),changes);
        return List.copyOf(changes);
    }
    private static void compare(String path,JsonNode b,JsonNode c,JsonNode n,List<Change> changes){
        if(Objects.equals(b,c)&&Objects.equals(b,n))return;
        if(b!=null&&c!=null&&n!=null&&b.isObject()&&c.isObject()&&n.isObject()){
            var fields=new TreeSet<String>();for(var value:List.of(b,c,n))value.properties().forEach(e->fields.add(e.getKey()));
            for(var field:fields)compare(path+"/"+field,b.get(field),c.get(field),n.get(field),changes);
        }else if(IDENTITIES.containsKey(path)&&b!=null&&c!=null&&n!=null&&b.isArray()&&c.isArray()&&n.isArray()){
            String key=IDENTITIES.get(path);var bm=index(b,key);var cm=index(c,key);var nm=index(n,key);
            var ids=new TreeSet<String>();ids.addAll(bm.keySet());ids.addAll(cm.keySet());ids.addAll(nm.keySet());
            for(var id:ids)compare(path+"/"+id,bm.get(id),cm.get(id),nm.get(id),changes);
        }else changes.add(new Change(path,b==null?NULL:b,c==null?NULL:c,n==null?NULL:n,
            !Objects.equals(b,c)&&!Objects.equals(b,n)&&!Objects.equals(c,n)));
    }
    private static Map<String,JsonNode> index(JsonNode array,String key){
        var result=new TreeMap<String,JsonNode>();for(var value:array)result.put(value.get(key).asString(),value);return result;
    }
}
