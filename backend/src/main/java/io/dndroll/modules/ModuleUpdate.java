package io.dndroll.modules;

import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Stable-identity three-way comparison. Preview has no persistence capability. */
public final class ModuleUpdate {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final JsonNode NULL=JSON.readTree("null");
    private static final Map<String,String> IDENTITIES=Map.of("/nodes","id","/play/scenes","nodeId","/play/units","nodeId","/play/placements","id","/manifest/assets","id","/manifest/dependencies","id","/operations/definitions","nodeId");
    public record Change(String path,JsonNode baseline,JsonNode current,JsonNode incoming,boolean conflict) {}
    public record Selection(String path,String choice) {}
    public static ModuleBlueprint.Document select(ModuleBlueprint.Document baseline,ModuleBlueprint.Document current,ModuleBlueprint.Document incoming,List<Selection> selections) {
        return JSON.treeToValue(selected(JSON.valueToTree(current),compare(baseline,current,incoming),selections),ModuleBlueprint.Document.class);
    }
    public record Prepared(ModuleBlueprint.Document document,ModuleArchive.Manifest manifest,Map<String,byte[]> images,List<Change> changes) {}
    public static List<Change> comparePackages(ModuleArchive.Verified baseline,ModuleArchive.Verified current,ModuleArchive.Verified incoming){
        var changes=new ArrayList<>(compare(baseline.document(),current.document(),incoming.document()));
        compare("/manifest",metadata(baseline.manifest()),metadata(current.manifest()),metadata(incoming.manifest()),changes);return List.copyOf(changes);
    }
    public static Prepared prepare(ModuleArchive.Verified baseline,ModuleArchive.Verified current,ModuleArchive.Verified incoming,Map<String,byte[]> oldImages,Map<String,byte[]> newImages,List<Selection> selections){
        var changes=comparePackages(baseline,current,incoming);
        var tree=(tools.jackson.databind.node.ObjectNode)JSON.valueToTree(current.document());tree.set("manifest",metadata(current.manifest()));
        var assembled=selected(tree,changes,selections);var document=(tools.jackson.databind.node.ObjectNode)assembled.deepCopy();document.remove("manifest");
        var m=(tools.jackson.databind.node.ObjectNode)JSON.valueToTree(current.manifest());assembled.get("manifest").properties().forEach(e->m.set(e.getKey(),e.getValue()));
        var manifest=JSON.treeToValue(m,ModuleArchive.Manifest.class);var images=new LinkedHashMap<String,byte[]>();
        for(var asset:manifest.assets()){
            byte[] bytes=oldImages.get(asset.id());if(bytes==null||!ModuleArchive.hash(bytes).equals(asset.sha256()))bytes=newImages.get(asset.id());
            if(bytes==null||!ModuleArchive.hash(bytes).equals(asset.sha256()))throw ModuleBlueprint.invalid();images.put(asset.id(),bytes);
        }
        var d=JSON.treeToValue(document,ModuleBlueprint.Document.class);ModuleBlueprint.validate(d,manifest);
        return new Prepared(d,manifest,images,List.copyOf(changes));
    }
    private static JsonNode metadata(ModuleArchive.Manifest m){return JSON.valueToTree(Map.of("rights",m.rights(),"assets",m.assets(),"dependencies",m.dependencies()));}
    private static JsonNode selected(JsonNode current,List<Change> changes,List<Selection> selections){
        if(selections==null||selections.size()!=changes.size())throw ModuleBlueprint.invalid();var choices=new HashMap<String,String>();
        for(var s:selections)if(s==null||s.path()==null||s.choice()==null||!Set.of("KEEP","INCOMING").contains(s.choice())||choices.put(s.path(),s.choice())!=null)throw ModuleBlueprint.invalid();
        var tree=current.deepCopy();for(var change:changes){String choice=choices.remove(change.path());if(choice==null)throw ModuleBlueprint.invalid();if(choice.equals("INCOMING"))replace(tree,"",change.path().substring(1).split("/"),0,change.incoming());}
        if(!choices.isEmpty())throw ModuleBlueprint.invalid();return tree;
    }
    private static void replace(JsonNode node,String path,String[] parts,int offset,JsonNode value){
        String key=parts[offset];boolean last=offset==parts.length-1;
        if(node.isObject()){
            var object=(tools.jackson.databind.node.ObjectNode)node;if(last){object.set(key,value);return;}replace(object.get(key),path+"/"+key,parts,offset+1,value);
        }else if(node.isArray()&&IDENTITIES.containsKey(path)){
            var array=(tools.jackson.databind.node.ArrayNode)node;String identity=IDENTITIES.get(path);int index=-1;for(int i=0;i<array.size();i++)if(array.get(i).get(identity).asString().equals(key)){index=i;break;}
            if(last){if(index>=0){if(value.isNull())array.remove(index);else array.set(index,value);}else if(!value.isNull())array.add(value);}
            else {if(index<0)throw ModuleBlueprint.invalid();replace(array.get(index),path+"/"+key,parts,offset+1,value);}
        }else throw ModuleBlueprint.invalid();
    }
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
