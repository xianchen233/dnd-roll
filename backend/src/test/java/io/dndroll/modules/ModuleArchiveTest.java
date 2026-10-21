package io.dndroll.modules;

import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.security.MessageDigest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;
import io.dndroll.domain.DomainException;
import static org.junit.jupiter.api.Assertions.*;

class ModuleArchiveTest {
    @TempDir Path root;
    final ModuleArchive validator=new ModuleArchive();
    final JsonMapper json=JsonMapper.builder().build();
    final String module="10000000-0000-4000-8000-000000000001",node="20000000-0000-4000-8000-000000000001";
    static String hash(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    Map<String,Object> node(){return new LinkedHashMap<>(Map.of("id",node,"kind","SCENE","name","测试场景","text","<script>literal text</script>","visibility","DM","nodeIds",List.of(),"assetIds",List.of(),"dependencyIds",List.of()));}
    Map<String,Object> manifest(byte[] content)throws Exception{return new LinkedHashMap<>(Map.of("format","dnd-roll.module","formatVersion",1,"moduleId",module,"moduleVersion","1.2.3","name","Synthetic module","rules","5R","rights","Synthetic data owned by test","dependencies",List.of(),"content",Map.of("size",content.length,"sha256",hash(content)),"assets",List.of()));}
    Path zip(Map<String,Object> manifest,byte[] content,Map<String,byte[]> extras)throws Exception{return zipText(json.writeValueAsBytes(manifest),content,extras);}
    Path zipText(byte[] manifest,byte[] content,Map<String,byte[]> extras)throws Exception{
        var path=root.resolve(UUID.randomUUID()+".zip");
        try(var zip=new ZipOutputStream(Files.newOutputStream(path))){
            for(var e:Map.of("manifest.json",manifest,"content.json",content).entrySet()){zip.putNextEntry(new ZipEntry(e.getKey()));zip.write(e.getValue());zip.closeEntry();}
            for(var e:extras.entrySet()){zip.putNextEntry(new ZipEntry(e.getKey()));zip.write(e.getValue());zip.closeEntry();}
        }return path;
    }
    byte[] content() {return json.writeValueAsBytes(Map.of("nodes",List.of(node())));}
    void invalid(Path path){var error=assertThrows(DomainException.class,()->validator.verify(path));assertEquals(400,error.status());assertEquals("INVALID_MODULE",error.code());}
    @Test void validPackageRetainsStableIdentityPrivateLiteralDataAndArchiveDigest()throws Exception{
        var content=content();var path=zip(manifest(content),content,Map.of());var result=validator.verify(path);
        assertEquals(module,result.manifest().moduleId());assertEquals("1.2.3",result.manifest().moduleVersion());
        assertEquals("DM",result.nodes().getFirst().visibility());assertEquals("<script>literal text</script>",result.nodes().getFirst().text());assertEquals(hash(Files.readAllBytes(path)),result.sha256());
    }
    @Test void permitsEmptyOptionalSectionsAndUnresolvedExternalDeclarations()throws Exception{
        byte[] content=json.writeValueAsBytes(Map.of("nodes",List.of()));var m=manifest(content);m.put("dependencies",List.of(Map.of("id",node,"version","2024-expansion-2","rules","2014")));
        assertEquals(1,validator.verify(zip(m,content,Map.of())).manifest().dependencies().size());
    }
    @Test void v2ExplicitlyCarriesEmptyTypedGameplayWithoutGuessingFromDescriptions()throws Exception{
        byte[] c=json.writeValueAsBytes(Map.of("nodes",List.of(node()),"play",Map.of("scenes",List.of(),"units",List.of(),"placements",List.of(),"admissionMode","REVIEW","approvedDependencyIds",List.of())));var m=manifest(c);m.put("formatVersion",2);
        assertEquals(2,validator.verify(zip(m,c,Map.of())).manifest().formatVersion());
    }
    @Test void v1RetainsItsOriginalFullContentByteLimitWhenPromotedToAnEditingBaseline()throws Exception{
        var nodes=new ArrayList<Map<String,Object>>();for(int i=0;i<104;i++){var n=node();n.put("id",UUID.randomUUID().toString());n.put("text","x".repeat(9900));nodes.add(n);}var last=node();last.put("id",UUID.randomUUID().toString());last.put("text","");nodes.add(last);
        int remaining=ModuleArchive.JSON_LIMIT-json.writeValueAsBytes(Map.of("nodes",nodes)).length;assertTrue(remaining>0&&remaining<=10000);last.put("text","x".repeat(remaining));byte[] bytes=json.writeValueAsBytes(Map.of("nodes",nodes));assertEquals(ModuleArchive.JSON_LIMIT,bytes.length);assertEquals(105,validator.verify(zip(manifest(bytes),bytes,Map.of())).nodes().size());
    }
    @Test void v1InertDocumentsRetainMoreThanFourImageReferences()throws Exception{
        var image=new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB);var out=new ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",out);byte[] bytes=out.toByteArray();var assets=new ArrayList<Map<String,Object>>();var files=new LinkedHashMap<String,byte[]>();var ids=new ArrayList<String>();
        for(int i=0;i<5;i++){String id=UUID.randomUUID().toString();ids.add(id);files.put("assets/"+id,bytes);assets.add(Map.of("id",id,"mediaType","image/png","size",bytes.length,"sha256",hash(bytes)));}var n=node();n.put("kind","DOCUMENT");n.put("assetIds",ids);byte[] content=json.writeValueAsBytes(Map.of("nodes",List.of(n)));var manifest=manifest(content);manifest.put("assets",assets);assertEquals(5,validator.verify(zip(manifest,content,files)).nodes().getFirst().assetIds().size());
    }
    @Test void rejectsWrongVersionsCoercedNumbersUnknownFieldsAndDuplicateJsonKeys()throws Exception{
        var content=content();
        for(var patch:List.of(Map.of("formatVersion",(Object)2),Map.of("formatVersion",(Object)"1"),Map.of("moduleVersion",(Object)"01.2.3"),Map.of("moduleVersion",(Object)"1.2"),Map.of("rules",(Object)"2024"),Map.of("script",(Object)"run()"),Map.of("moduleId",(Object)"1-1-1-1-1"))){var m=manifest(content);m.putAll(patch);invalid(zip(m,content,Map.of()));}
        String duplicate=json.writeValueAsString(manifest(content)).replace("\"formatVersion\":1","\"formatVersion\":1,\"formatVersion\":1");invalid(zipText(duplicate.getBytes(java.nio.charset.StandardCharsets.UTF_8),content,Map.of()));
    }
    @Test void rejectsExecutableExtraFilesAndHostPathsWithoutExtracting()throws Exception{
        var content=content();for(String extra:List.of("../escape","/absolute","C:/absolute","assets\\evil","assets/../evil","logic.js","empty/"))invalid(zip(manifest(content),content,Map.of(extra,new byte[]{1})));
        assertFalse(Files.exists(root.resolve("escape")));
    }
    @Test void rejectsCorruptContentAndOversizedExpandedText()throws Exception{
        var content=content();invalid(zip(manifest(content),new byte[]{1,2,3},Map.of()));
        var n=node();n.put("text","x".repeat(10001));byte[] oversized=json.writeValueAsBytes(Map.of("nodes",List.of(n)));invalid(zip(manifest(oversized),oversized,Map.of()));
        byte[] huge=" ".repeat(1024*1024+1).getBytes();invalid(zip(manifest(huge),huge,Map.of()));
    }
    @Test void rejectsDuplicateIdentitiesDanglingAndDuplicateReferences()throws Exception{
        var n=node();for(String field:List.of("nodeIds","assetIds","dependencyIds")){n=node();n.put(field,List.of(module));byte[] content=json.writeValueAsBytes(Map.of("nodes",List.of(n)));invalid(zip(manifest(content),content,Map.of()));}
        n=node();n.put("nodeIds",List.of(node,node));byte[] content=json.writeValueAsBytes(Map.of("nodes",List.of(n)));invalid(zip(manifest(content),content,Map.of()));
        content=json.writeValueAsBytes(Map.of("nodes",List.of(node(),node())));invalid(zip(manifest(content),content,Map.of()));
    }
    @Test void validatesActualPngBytesHashesAndAllAssetReferences()throws Exception{
        var image=new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB);var out=new ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",out);byte[] bytes=out.toByteArray();
        var n=node();n.put("assetIds",List.of(module));byte[] content=json.writeValueAsBytes(Map.of("nodes",List.of(n)));var m=manifest(content);m.put("assets",List.of(Map.of("id",module,"mediaType","image/png","size",bytes.length,"sha256",hash(bytes))));
        assertEquals(1,validator.verify(zip(m,content,Map.of("assets/"+module,bytes))).manifest().assets().size());
        invalid(zip(m,content,Map.of()));invalid(zip(m,content,Map.of("assets/"+module,new byte[]{1,2,3})));
        byte[] fake=new byte[]{1,2,3};m.put("assets",List.of(Map.of("id",module,"mediaType","image/png","size",fake.length,"sha256",hash(fake))));invalid(zip(m,content,Map.of("assets/"+module,fake)));
    }
}
