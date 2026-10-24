package io.dndroll.persistence;

import io.dndroll.application.AssetService;
import io.dndroll.domain.DomainException;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PrivateAssetFiles implements AssetService.Files {
    private final Path root;
    public PrivateAssetFiles(@Value("${assets.storage-root}") String root) { this.root = Path.of(root).toAbsolutePath().normalize(); }
    private Path path(String key) {
        try {
            if (key == null || !UUID.fromString(key).toString().equals(key)) throw unavailable();
            return root.resolve(key);
        } catch (IllegalArgumentException failure) { throw unavailable(); }
    }
    public void publish(String key, byte[] bytes) {
        Path temporary = null;
        try {
            java.nio.file.Files.createDirectories(root);
            temporary = java.nio.file.Files.createTempFile(root, ".upload-", ".tmp");
            java.nio.file.Files.write(temporary, bytes);
            // Require an atomic rename on the same private filesystem. Never fall back to a partial copy.
            java.nio.file.Files.move(temporary, path(key), StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException failure) { throw unavailable(); }
        finally {
            if (temporary != null) try { java.nio.file.Files.deleteIfExists(temporary); }
            catch (IOException failure) { throw unavailable(); }
        }
    }
    public byte[] read(String key) {
        try {
            Path target = path(key);
            if (!java.nio.file.Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) throw unavailable();
            return java.nio.file.Files.readAllBytes(target);
        } catch (IOException failure) { throw unavailable(); }
    }
    public void discard(String key) {
        try { java.nio.file.Files.deleteIfExists(path(key)); }
        catch (IOException failure) { throw unavailable(); }
    }
    private static DomainException unavailable() { return new DomainException("ASSET_UNAVAILABLE", 503, "Asset is unavailable"); }
}
