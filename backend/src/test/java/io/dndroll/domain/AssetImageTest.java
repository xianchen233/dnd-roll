package io.dndroll.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import java.util.*;
import org.junit.jupiter.api.Test;

class AssetImageTest {
    public static byte[] image(String format, int width, int height) {
        try {
            var out = new ByteArrayOutputStream();
            assertTrue(ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), format, out));
            return out.toByteArray();
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
    @Test void validatedMetadataAndHashComeFromActualDecodedPngAndJpeg() {
        for (var format : List.of("png", "jpeg")) {
            byte[] bytes = image(format, 3, 2);
            var validated = AssetImage.validate(bytes, "image/" + format);
            assertEquals(3, validated.width()); assertEquals(2, validated.height());
            assertEquals("image/" + format, validated.mediaType());
            assertArrayEquals(bytes, validated.bytes()); assertEquals(64, validated.sha256().length());
        }
    }
    @Test void malformedEmptySvgMimeMismatchAndTruncationAreRejected() {
        for (byte[] bytes : List.of(new byte[0], "<svg/>".getBytes(), new byte[]{1,2,3}, Arrays.copyOf(image("png", 2, 2), 35)))
            invalid(bytes, "image/png");
        byte[] png = image("png", 2, 2), jpeg = image("jpeg", 2, 2);
        invalid(png, "image/jpeg"); invalid(jpeg, "image/png"); invalid(png, "image/svg+xml");
        invalid(Arrays.copyOf(jpeg, jpeg.length - 2), "image/jpeg");
        invalid(Arrays.copyOf(png, png.length - 12), "image/png");
    }
    @Test void byteDimensionAndPixelBoundsAreEnforcedBeforeFullDecode() {
        invalid(new byte[AssetImage.MAX_BYTES + 1], "image/png");
        invalid(image("png", 8193, 1), "image/png");
        invalid(image("png", 8000, 4001), "image/png");
        assertEquals(8192, AssetImage.validate(image("png", 8192, 1), "image/png").width());
    }
    static void invalid(byte[] bytes, String mediaType) {
        var error = assertThrows(DomainException.class, () -> AssetImage.validate(bytes, mediaType));
        assertEquals("INVALID_INPUT", error.code()); assertEquals(400, error.status());
    }
}
