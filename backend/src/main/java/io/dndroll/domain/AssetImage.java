package io.dndroll.domain;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;

/** Validated bytes; constructed only by the image boundary. */
public record AssetImage(byte[] bytes, String mediaType, int width, int height, String sha256) {
    public static final int MAX_BYTES = 10 * 1024 * 1024;
    public static AssetImage validate(byte[] bytes, String mediaType) {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES
            || !("image/png".equals(mediaType) || "image/jpeg".equals(mediaType))) throw invalid();
        // ImageIO can tolerate a missing end marker. Reject incomplete containers first.
        if (mediaType.equals("image/png")) pngContainer(bytes);
        else if (bytes.length < 4 || (bytes[0] & 255) != 255 || (bytes[1] & 255) != 216
            || (bytes[bytes.length - 2] & 255) != 255 || (bytes[bytes.length - 1] & 255) != 217) throw invalid();
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalid();
            var reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!(mediaType.equals("image/png") ? format.equals("png") : format.equals("jpeg"))) throw invalid();
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 8192 || height > 8192 || (long) width * height > 32_000_000) throw invalid();
                reader.addIIOReadWarningListener((source, warning) -> { throw invalid(); });
                var image = reader.read(0);
                if (image == null || image.getWidth() != width || image.getHeight() != height) throw invalid();
                return new AssetImage(bytes.clone(), mediaType, width, height,
                    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
            } finally { reader.dispose(); }
        } catch (IOException | NoSuchAlgorithmException | IllegalArgumentException failure) { throw invalid(); }
    }
    private static void pngContainer(byte[] bytes) {
        byte[] signature = {(byte)137,80,78,71,13,10,26,10};
        if (bytes.length < 20 || !Arrays.equals(signature, Arrays.copyOf(bytes, 8))) throw invalid();
        int position = 8;
        while (position <= bytes.length - 12) {
            int length = intAt(bytes, position);
            if (length < 0 || length > bytes.length - position - 12) throw invalid();
            var crc = new CRC32(); crc.update(bytes, position + 4, length + 4);
            if ((int)crc.getValue() != intAt(bytes, position + length + 8)) throw invalid();
            boolean end = bytes[position+4] == 'I' && bytes[position+5] == 'E' && bytes[position+6] == 'N' && bytes[position+7] == 'D';
            position += length + 12;
            if (end) { if (length != 0 || position != bytes.length) throw invalid(); return; }
        }
        throw invalid();
    }
    private static int intAt(byte[] bytes, int offset) {
        return (bytes[offset] & 255) << 24 | (bytes[offset+1] & 255) << 16 | (bytes[offset+2] & 255) << 8 | bytes[offset+3] & 255;
    }
    private static DomainException invalid() { return new DomainException("INVALID_INPUT", 400, "Invalid PNG or JPEG image"); }
}
