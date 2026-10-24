package io.dndroll.backup;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

class BackupArchiveTest {
    @TempDir Path root;
    final String key = UUID.randomUUID().toString();
    final byte[] dump = "PGDMP synthetic archive".getBytes(), image = {1,2,3};
    final Map<String,Integer> migrations = Map.of("1",123,"17",456);
    final BackupArchive validator = new BackupArchive();
    static String hash(byte[] data) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)); }
    BackupArchive.Manifest manifest() throws Exception {
        return new BackupArchive.Manifest("dnd-roll.backup",1,"2026-10-21T12:00:00Z",18,migrations,
            Map.of("rp_message_history",new BackupArchive.Table(2,hash(image))),
            new BackupArchive.File("database.dump",dump.length,hash(dump)),
            List.of(new BackupArchive.File(key,image.length,hash(image))));
    }
    Path zip(BackupArchive.Manifest manifest, Map<String,byte[]> entries) throws Exception {
        return zipText(JsonMapper.builder().build().writeValueAsString(manifest),entries);
    }
    Path zipText(String manifest, Map<String,byte[]> entries) throws Exception {
        var path = root.resolve(UUID.randomUUID()+".zip");
        try (var out = new ZipOutputStream(Files.newOutputStream(path))) {
            out.putNextEntry(new ZipEntry("manifest.json")); out.write(manifest.getBytes(java.nio.charset.StandardCharsets.UTF_8)); out.closeEntry();
            for(var e:entries.entrySet()) { out.putNextEntry(new ZipEntry(e.getKey())); out.write(e.getValue()); out.closeEntry(); }
        }
        return path;
    }
    @Test void archiveVersionsAndLengthsRequireExactJsonTypes() throws Exception {
        String valid=JsonMapper.builder().build().writeValueAsString(manifest());
        for(String malformed:List.of(valid.replace("\"formatVersion\":1","\"formatVersion\":\"1\""),valid.replace("\"databaseMajor\":18","\"databaseMajor\":18.1"),valid.replace("\"size\":3","\"size\":\"3\"")))
            assertThrows(BackupArchive.InvalidArchive.class,()->validator.verify(zipText(malformed,entries()),18,migrations));
    }
    Map<String,byte[]> entries() { return new HashMap<>(Map.of("database.dump",dump,"assets/"+key,image)); }
    @Test void validPortableArchivePreservesVersionsHistoryAndFileIdentity() throws Exception {
        var manifest = manifest();
        assertEquals(manifest,validator.verify(zip(manifest,entries()),18,migrations));
    }
    @Test void writesOnlyCommittedImmutableFilesAndRefusesChangedBytes() throws Exception {
        var assets=Files.createDirectory(root.resolve("live"));
        Files.write(assets.resolve(key),image);Files.write(assets.resolve(UUID.randomUUID().toString()),new byte[]{8});
        var database=Files.write(root.resolve("database.dump"),dump);var archive=root.resolve("completed.zip");
        validator.write(archive,manifest(),database,assets);
        assertEquals(manifest(),validator.verify(archive,18,migrations));
        try(var zip=new ZipFile(archive.toFile())) {assertEquals(3,zip.size());}
        assertThrows(IOException.class,()->validator.write(archive,manifest(),database,assets),"never overwrite a completed archive");
        Files.write(assets.resolve(key),new byte[]{7});
        assertThrows(BackupArchive.InvalidArchive.class,()->validator.write(root.resolve("changed.zip"),manifest(),database,assets));
        assertArrayEquals(new byte[]{7},Files.readAllBytes(assets.resolve(key)),"failure never deletes live files");
    }
    @Test void rejectsMissingExtraAndTraversalEntries() throws Exception {
        for(String extra:List.of("../escape","/absolute","assets\\escape","unexpected","assets/../escape")) {
            var entries=entries();entries.put(extra,image);
            assertThrows(BackupArchive.InvalidArchive.class,()->validator.verify(zip(manifest(),entries),18,migrations),extra);
        }
        var missing=entries();missing.remove("assets/"+key);
        assertThrows(BackupArchive.InvalidArchive.class,()->validator.verify(zip(manifest(),missing),18,migrations));
    }
    @Test void rejectsCorruptionAndTruncationOfDumpAndHistoricalImages() throws Exception {
        for(String name:List.of("database.dump","assets/"+key)) {
            var entries=entries();entries.put(name,new byte[]{9,9,9});
            assertThrows(BackupArchive.InvalidArchive.class,()->validator.verify(zip(manifest(),entries),18,migrations));
        }
        Path truncated=root.resolve("broken.zip");Files.write(truncated,new byte[]{80,75,0});
        assertThrows(BackupArchive.InvalidArchive.class,()->validator.verify(truncated,18,migrations));
    }
    @Test void rejectsDatabaseAndSchemaMismatchAndDuplicateAssetIdentity() throws Exception {
        var archive=zip(manifest(),entries());
        assertThrows(BackupArchive.InvalidArchive.class,()->validator.verify(archive,17,migrations));
        assertThrows(BackupArchive.InvalidArchive.class,()->validator.verify(archive,18,Map.of("1",999,"17",456)));
        var m=manifest();var duplicate=new BackupArchive.Manifest(m.format(),1,m.createdAt(),18,migrations,m.tables(),m.database(),List.of(m.assets().getFirst(),m.assets().getFirst()));
        assertThrows(BackupArchive.InvalidArchive.class,()->validator.verify(zip(duplicate,entries()),18,migrations));
        var future=new BackupArchive.Manifest(m.format(),2,m.createdAt(),18,migrations,m.tables(),m.database(),m.assets());
        assertThrows(BackupArchive.InvalidArchive.class,()->validator.verify(zip(future,entries()),18,migrations));
    }
}
