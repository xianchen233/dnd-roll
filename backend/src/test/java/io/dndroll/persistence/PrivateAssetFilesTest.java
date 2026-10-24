package io.dndroll.persistence;

import static org.junit.jupiter.api.Assertions.*;
import io.dndroll.domain.DomainException;
import java.nio.file.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PrivateAssetFilesTest {
    @TempDir Path root;
    @Test void atomicPublicationAndNewAdapterPreserveOriginalBytesWithoutTemporaryFiles() throws Exception {
        var storage=new PrivateAssetFiles(root.toString());String key=UUID.randomUUID().toString();byte[] bytes={1,2,3};
        storage.publish(key,bytes);assertArrayEquals(bytes,new PrivateAssetFiles(root.toString()).read(key));
        try(var entries=Files.list(root)){assertEquals(java.util.List.of(key),entries.map(p->p.getFileName().toString()).toList());}
        storage.discard(key);assertFalse(Files.exists(root.resolve(key)));
    }
    @Test void missingUnreadableAndUnsafeKeysAreGenericWithoutPaths() throws Exception {
        var storage=new PrivateAssetFiles(root.toString());
        for(String key:java.util.List.of(UUID.randomUUID().toString(),"../outside","not-a-key")) {
            var error=assertThrows(DomainException.class,()->storage.read(key));assertEquals(503,error.status());assertEquals("ASSET_UNAVAILABLE",error.code());assertFalse(error.getMessage().contains(root.toString()));
        }
        String directory=UUID.randomUUID().toString();Files.createDirectory(root.resolve(directory));assertThrows(DomainException.class,()->storage.read(directory));
    }
    @Test void publicationFailureLeavesNoFilesAndDoesNotTouchExistingFiles() throws Exception {
        Path file=Files.createFile(root.resolve("not-a-directory"));var storage=new PrivateAssetFiles(file.toString());
        var error=assertThrows(DomainException.class,()->storage.publish(UUID.randomUUID().toString(),new byte[]{1}));assertEquals("ASSET_UNAVAILABLE",error.code());assertTrue(Files.exists(file));
        try(var entries=Files.list(root)){assertEquals(1,entries.count());}
    }
}
