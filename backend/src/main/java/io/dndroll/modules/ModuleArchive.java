package io.dndroll.modules;

import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.security.*;
import io.dndroll.domain.AssetImage;
import io.dndroll.domain.DomainException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The package boundary produces inert validated data, never executable content. */
public final class ModuleArchive {
    public record Dependency(String id,String version,String rules) {}
    public record Content(long size,String sha256) {}
    public record Asset(String id,String mediaType,long size,String sha256) {}
    public record Manifest(String format,int formatVersion,String moduleId,String moduleVersion,
        String name,String rules,String rights,List<Dependency> dependencies,Content content,List<Asset> assets) {}
    public record Node(String id,String kind,String name,String text,String visibility,List<String> nodeIds,List<String> assetIds,List<String> dependencyIds) {}
    public record Verified(Manifest manifest,List<Node> nodes,String sha256) {}
    public static final int MAX_BYTES=10*1024*1024, JSON_LIMIT=1024*1024;
    private static final long EXPANDED_LIMIT=32L*1024*1024;
    private final JsonMapper json=JsonMapper.builder().enable(tools.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    public Verified verify(Path archive) {
        try {
            if(!Files.isRegularFile(archive,LinkOption.NOFOLLOW_LINKS)||Files.size(archive)<1||Files.size(archive)>MAX_BYTES)throw invalid();
            try(var zip=new ZipFile(archive.toFile())) {
                var names=new HashSet<String>();long expanded=0;
                for(var entries=zip.entries();entries.hasMoreElements();) {
                    var e=entries.nextElement();
                    if(e.isDirectory()||e.getSize()<1||e.getSize()>MAX_BYTES||!names.add(e.getName())||names.size()>130)throw invalid();
                    expanded+=e.getSize();if(expanded>EXPANDED_LIMIT)throw invalid();
                }
                JsonNode m=readJson(zip,"manifest.json");
                object(m,"format","formatVersion","moduleId","moduleVersion","name","rules","rights","dependencies","content","assets");
                if(!text(m.get("format"),1,40).equals("dnd-roll.module")||number(m.get("formatVersion"),1,1)!=1)throw invalid();
                id(m.get("moduleId"));String version=text(m.get("moduleVersion"),5,20);
                if(!version.matches("(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})"))throw invalid();
                text(m.get("name"),1,100);rules(m.get("rules"));text(m.get("rights"),1,1000);
                file(m.get("content"),false);array(m.get("dependencies"),100);array(m.get("assets"),128);
                var dependencies=new HashSet<String>();var assets=new HashSet<String>();
                for(var d:m.get("dependencies")){object(d,"id","version","rules");if(!dependencies.add(id(d.get("id"))))throw invalid();text(d.get("version"),1,80);rules(d.get("rules"));}
                for(var a:m.get("assets")){file(a,true);if(!assets.add(id(a.get("id"))))throw invalid();}
                var manifest=json.treeToValue(m,Manifest.class);
                var expected=new HashSet<>(Set.of("manifest.json","content.json"));for(String asset:assets)expected.add("assets/"+asset);
                if(!names.equals(expected))throw invalid();
                byte[] content=bytes(zip,"content.json",manifest.content().size(),JSON_LIMIT,manifest.content().sha256());
                JsonNode c=json.readTree(content);object(c,"nodes");array(c.get("nodes"),500);var ids=new HashSet<String>();
                for(var n:c.get("nodes")) {
                    object(n,"id","kind","name","text","visibility","nodeIds","assetIds","dependencyIds");
                    if(!ids.add(id(n.get("id")))||!Set.of("CHAPTER","SCENE","UNIT","ENCOUNTER","DOCUMENT","REWARD","TRIGGER").contains(text(n.get("kind"),1,20))
                        ||!Set.of("DM","PUBLIC").contains(text(n.get("visibility"),1,6)))throw invalid();
                    text(n.get("name"),1,100);text(n.get("text"),0,10000);
                }
                var nodes=new ArrayList<Node>();
                for(var n:c.get("nodes")){references(n.get("nodeIds"),ids);references(n.get("assetIds"),assets);references(n.get("dependencyIds"),dependencies);nodes.add(json.treeToValue(n,Node.class));}
                for(var a:manifest.assets())AssetImage.validate(bytes(zip,"assets/"+a.id(),a.size(),MAX_BYTES,a.sha256()),a.mediaType());
                try(var in=Files.newInputStream(archive,LinkOption.NOFOLLOW_LINKS)){return new Verified(manifest,List.copyOf(nodes),hash(in.readNBytes(MAX_BYTES+1)));}
            }
        } catch(IOException|RuntimeException e) {throw invalid();}
    }
    private JsonNode readJson(ZipFile zip,String name)throws IOException {
        var e=zip.getEntry(name);if(e==null||e.getSize()>JSON_LIMIT)throw invalid();
        try(var in=zip.getInputStream(e)){byte[] b=in.readNBytes(JSON_LIMIT+1);if(b.length>JSON_LIMIT)throw invalid();return json.readTree(b);}
    }
    private static byte[] bytes(ZipFile zip,String name,long size,int max,String sha)throws IOException {
        var e=zip.getEntry(name);if(e==null||size<1||size>max||e.getSize()!=size)throw invalid();
        try(var in=zip.getInputStream(e)){byte[] b=in.readNBytes((int)size+1);if(b.length!=size||!hash(b).equals(sha))throw invalid();return b;}
    }
    private static void object(JsonNode n,String... fields){if(n==null||!n.isObject()||n.size()!=fields.length)throw invalid();for(String field:fields)if(!n.has(field)||n.get(field).isNull())throw invalid();}
    private static void array(JsonNode n,int max){if(n==null||!n.isArray()||n.size()>max)throw invalid();}
    private static String text(JsonNode n,int min,int max){if(n==null||!n.isString())throw invalid();String s=n.asString();if(s.length()<min||s.length()>max||(min>0&&s.isBlank())||s.indexOf('\0')>=0)throw invalid();return s;}
    private static long number(JsonNode n,long min,long max){if(n==null||!n.isIntegralNumber()||!n.canConvertToLong()||n.asLong()<min||n.asLong()>max)throw invalid();return n.asLong();}
    private static String id(JsonNode n){String s=text(n,36,36);if(!UUID.fromString(s).toString().equals(s))throw invalid();return s;}
    private static void rules(JsonNode n){if(!Set.of("5R","2014","CUSTOM").contains(text(n,1,10)))throw invalid();}
    private static void file(JsonNode n,boolean asset){
        if(asset){object(n,"id","mediaType","size","sha256");id(n.get("id"));if(!Set.of("image/png","image/jpeg").contains(text(n.get("mediaType"),1,20)))throw invalid();}
        else object(n,"size","sha256");
        number(n.get("size"),1,asset?MAX_BYTES:JSON_LIMIT);if(!text(n.get("sha256"),64,64).matches("[a-f0-9]{64}"))throw invalid();
    }
    private static void references(JsonNode n,Set<String> known){array(n,500);var used=new HashSet<String>();for(var ref:n){String id=id(ref);if(!used.add(id)||!known.contains(id))throw invalid();}}
    public static String hash(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
    private static DomainException invalid(){return new DomainException("INVALID_MODULE",400,"Invalid or incompatible module package");}
}
