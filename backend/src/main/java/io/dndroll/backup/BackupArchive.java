package io.dndroll.backup;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.time.Instant;
import java.util.*;
import java.util.zip.*;
import tools.jackson.databind.json.JsonMapper;

/** Portable archive identities never contain host paths or environment credentials. */
public final class BackupArchive {
    public record File(String key, long size, String sha256) {}
    public record Table(long rows, String sha256) {}
    public record Manifest(String format, int formatVersion, String createdAt, int databaseMajor,
        Map<String,Integer> migrations, Map<String,Table> tables, File database, List<File> assets) {}
    public static class InvalidArchive extends IOException {
        public InvalidArchive() { super("Invalid or incompatible backup archive"); }
    }
    private static final int MANIFEST_LIMIT = 4 * 1024 * 1024;
    private static final long DUMP_LIMIT = 1L << 40;
    private final JsonMapper json = JsonMapper.builder()
        .enable(tools.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    public void write(Path archive, Manifest manifest, Path dump, Path assetRoot) throws IOException {
        validate(manifest, manifest.databaseMajor(), manifest.migrations());
        if (!Files.isDirectory(assetRoot, LinkOption.NOFOLLOW_LINKS)) throw new InvalidArchive();
        boolean created = false;
        try {
            var stream = Files.newOutputStream(archive, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            created = true;
            try (var zip = new ZipOutputStream(stream)) {
                zip.putNextEntry(new ZipEntry("manifest.json")); zip.write(json.writeValueAsBytes(manifest)); zip.closeEntry();
                copy(zip, "database.dump", dump, manifest.database());
                var keys = new HashSet<String>();
                for (var file : manifest.assets()) {
                    if (!keys.add(file.key())) throw new InvalidArchive();
                    copy(zip, "assets/" + file.key(), assetRoot.resolve(file.key()), file);
                }
            }
            verify(archive, manifest.databaseMajor(), manifest.migrations());
        } catch (IOException | RuntimeException failure) {
            if (created) Files.deleteIfExists(archive);
            throw failure;
        }
    }
    private static void copy(ZipOutputStream zip, String name, Path source, File expected) throws IOException {
        if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS) || Files.size(source) != expected.size()) throw new InvalidArchive();
        zip.putNextEntry(new ZipEntry(name));
        try (var in = Files.newInputStream(source, LinkOption.NOFOLLOW_LINKS)) {
            var digest = sha256(); long size = 0; byte[] buffer = new byte[65536];
            for (int n; (n = in.read(buffer)) != -1;) {
                size += n; if (size > expected.size()) throw new InvalidArchive();
                zip.write(buffer,0,n); digest.update(buffer,0,n);
            }
            if (size != expected.size() || !HexFormat.of().formatHex(digest.digest()).equals(expected.sha256())) throw new InvalidArchive();
        }
        zip.closeEntry();
    }
    public Manifest verify(Path archive, int major, Map<String,Integer> migrations) throws IOException {
        try (var zip = new ZipFile(archive.toFile())) {
            var names = new HashSet<String>();
            var enumeration = zip.entries();
            while (enumeration.hasMoreElements()) {
                var entry = enumeration.nextElement();
                if (names.size() > 100002 || entry.isDirectory() || !names.add(entry.getName())) throw new InvalidArchive();
            }
            var manifestEntry = zip.getEntry("manifest.json");
            if (manifestEntry == null || manifestEntry.getSize() < 1 || manifestEntry.getSize() > MANIFEST_LIMIT) throw new InvalidArchive();
            Manifest manifest;
            try (var in = zip.getInputStream(manifestEntry)) {
                var bytes = in.readNBytes(MANIFEST_LIMIT + 1);
                if (bytes.length > MANIFEST_LIMIT) throw new InvalidArchive();
                var node=json.readTree(bytes);shape(node);manifest = json.treeToValue(node, Manifest.class);
            }
            validate(manifest, major, migrations);
            var expected = new HashSet<>(Set.of("manifest.json", "database.dump"));
            for (var file : manifest.assets()) if (!expected.add("assets/" + file.key())) throw new InvalidArchive();
            if (!expected.equals(names)) throw new InvalidArchive();
            verifyFile(zip, "database.dump", manifest.database());
            for (var file : manifest.assets()) verifyFile(zip, "assets/" + file.key(), file);
            return manifest;
        } catch (InvalidArchive failure) { throw failure; }
        catch (IOException | RuntimeException failure) { throw new InvalidArchive(); }
    }
    private static void shape(tools.jackson.databind.JsonNode n)throws InvalidArchive {
        object(n,Set.of("format","formatVersion","createdAt","databaseMajor","migrations","tables","database","assets"));
        if(!n.get("format").isString()||!n.get("createdAt").isString())throw new InvalidArchive();
        integer(n.get("formatVersion"),true);integer(n.get("databaseMajor"),true);
        if(!n.get("migrations").isObject()||!n.get("tables").isObject()||!n.get("assets").isArray())throw new InvalidArchive();
        for(var entry:n.get("migrations").properties())integer(entry.getValue(),true);
        for(var entry:n.get("tables").properties()){
            object(entry.getValue(),Set.of("rows","sha256"));integer(entry.getValue().get("rows"),false);
            if(!entry.getValue().get("sha256").isString())throw new InvalidArchive();
        }
        fileShape(n.get("database"));for(var file:n.get("assets"))fileShape(file);
    }
    private static void fileShape(tools.jackson.databind.JsonNode file)throws InvalidArchive {
        object(file,Set.of("key","size","sha256"));integer(file.get("size"),false);
        if(!file.get("key").isString()||!file.get("sha256").isString())throw new InvalidArchive();
    }
    private static void integer(tools.jackson.databind.JsonNode value,boolean small)throws InvalidArchive {
        if(value==null||!value.isIntegralNumber()||!value.canConvertToLong()||(small&&!value.canConvertToInt()))throw new InvalidArchive();
    }
    private static void object(tools.jackson.databind.JsonNode n,Set<String> keys)throws InvalidArchive {
        if(n==null||!n.isObject()||n.size()!=keys.size())throw new InvalidArchive();
        for(String key:keys)if(!n.has(key)||n.get(key).isNull())throw new InvalidArchive();
    }
    private void validate(Manifest m, int major, Map<String,Integer> migrations) throws InvalidArchive {
        try {
            if (!"dnd-roll.backup".equals(m.format()) || m.formatVersion() != 1 || major != 18 || m.databaseMajor() != major
                || m.migrations() == null || m.migrations().isEmpty() || !m.migrations().equals(migrations)
                || m.tables() == null || m.tables().isEmpty() || m.tables().size() > 1000 || m.assets() == null || m.assets().size() > 100000) throw new InvalidArchive();
            Instant.parse(m.createdAt());
            for (var e : m.migrations().entrySet()) if (!e.getKey().matches("[1-9][0-9]*") || e.getValue() == null) throw new InvalidArchive();
            for (var e : m.tables().entrySet()) if (!e.getKey().matches("[a-z_][a-z0-9_]{0,62}") || e.getValue() == null
                || e.getValue().rows() < 0 || !sha(e.getValue().sha256())) throw new InvalidArchive();
            validateFile(m.database(), DUMP_LIMIT);
            if (!m.database().key().equals("database.dump")) throw new InvalidArchive();
            for (var file : m.assets()) {
                validateFile(file, 10485760);
                if (!UUID.fromString(file.key()).toString().equals(file.key())) throw new InvalidArchive();
            }
        } catch (InvalidArchive failure) { throw failure; }
        catch (RuntimeException failure) { throw new InvalidArchive(); }
    }
    private static boolean sha(String hash) { return hash != null && hash.matches("[a-f0-9]{64}"); }
    private static void validateFile(File file, long limit) throws InvalidArchive {
        if (file == null || file.key() == null || file.size() < 1 || file.size() > limit || !sha(file.sha256())) throw new InvalidArchive();
    }
    private static void verifyFile(ZipFile zip, String name, File file) throws IOException {
        var entry = zip.getEntry(name);
        if (entry == null || entry.getSize() != file.size()) throw new InvalidArchive();
        try (var in = zip.getInputStream(entry)) {
            var actual = digest(in, file.size());
            if (!actual.equals(file.sha256())) throw new InvalidArchive();
        }
    }
    static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    static String digest(InputStream in, long expectedSize) throws IOException {
        var digest = sha256(); long size = 0; byte[] buffer = new byte[65536];
        for (int n; (n = in.read(buffer)) != -1;) {
            size += n; if (size > expectedSize) throw new InvalidArchive();
            digest.update(buffer, 0, n);
        }
        if (size != expectedSize) throw new InvalidArchive();
        return HexFormat.of().formatHex(digest.digest());
    }
}
