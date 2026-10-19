package io.dndroll.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import org.junit.jupiter.api.Test;

class SafeMapImageTest {
    byte[] encode(BufferedImage image) throws Exception {var bytes=new ByteArrayOutputStream();ImageIO.write(image,"png",bytes);return bytes.toByteArray();}
    BufferedImage decode(byte[] bytes)throws Exception{try(var input=new javax.imageio.stream.MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))){var reader=ImageIO.getImageReaders(input).next();try{reader.setInput(input);return reader.read(0);}finally{reader.dispose();}}}
    @Test void everyCoveredPixelIsOpaqueBlackEvenBesidePublicSamples(){
        assertDoesNotThrow(()->{var source=new BufferedImage(16,8,BufferedImage.TYPE_INT_RGB);for(int y=0;y<8;y++)for(int x=0;x<16;x++)source.setRGB(x,y,x<8?0xff23ab45:0xffef12cd);
            var result=decode(SafeMapImage.render(encode(source),2,1,48,8,0,0,(x,y)->x==0));
            for(int y=0;y<48;y++)for(int x=0;x<96;x++)assertEquals(x<48?0xff23ab45:0xff000000,result.getRGB(x,y));});
    }
    @Test void calibrationUsesFloorAndNegativeOffsetsWithBlackOutOfBounds()throws Exception{
        var source=new BufferedImage(4,3,BufferedImage.TYPE_INT_RGB);for(int y=0;y<3;y++)for(int x=0;x<4;x++)source.setRGB(x,y,0xff000000|((x+1)<<16)|((y+1)<<8));
        var result=decode(SafeMapImage.render(encode(source),2,2,5,3,-1,-1,(x,y)->true));
        for(int y=0;y<10;y++)for(int x=0;x<10;x++){int sx=-1+x*3/5,sy=-1+y*3/5;assertEquals(sx<0||sy<0||sx>=4||sy>=3?0xff000000:source.getRGB(sx,sy),result.getRGB(x,y),x+","+y);}
    }
    @Test void allRevealedIsStillFreshRgbPngWithoutTextMetadataOrInvisibleSourceRgb()throws Exception{
        var source=new BufferedImage(3,1,BufferedImage.TYPE_INT_ARGB);source.setRGB(0,0,0x00ef12cd);source.setRGB(1,0,0x80fe8040);source.setRGB(2,0,0xff123456);
        var writer=ImageIO.getImageWritersByFormatName("png").next();byte[] original;
        try(var bytes=new ByteArrayOutputStream();var output=new MemoryCacheImageOutputStream(bytes)){
            writer.setOutput(output);var metadata=writer.getDefaultImageMetadata(new ImageTypeSpecifier(source),writer.getDefaultWriteParam());
            var root=new IIOMetadataNode("javax_imageio_png_1.0");var text=new IIOMetadataNode("tEXt");var entry=new IIOMetadataNode("tEXtEntry");entry.setAttribute("keyword","Secret");entry.setAttribute("value","DM_ONLY_METADATA_SECRET");text.appendChild(entry);root.appendChild(text);metadata.mergeTree("javax_imageio_png_1.0",root);
            writer.write(null,new IIOImage(source,null,metadata),null);output.flush();original=bytes.toByteArray();
        }finally{writer.dispose();}
        assertTrue(new String(original,java.nio.charset.StandardCharsets.ISO_8859_1).contains("DM_ONLY_METADATA_SECRET"));
        var output=SafeMapImage.render(original,3,1,1,1,0,0,(x,y)->true);var result=decode(output);
        assertFalse(Arrays.equals(original,output));assertFalse(new String(output,java.nio.charset.StandardCharsets.ISO_8859_1).contains("DM_ONLY_METADATA_SECRET"));
        assertEquals(0xff000000,result.getRGB(0,0));assertEquals(0xff7f4020,result.getRGB(1,0));assertEquals(0xff123456,result.getRGB(2,0));assertFalse(result.getColorModel().hasAlpha());
        var reader=ImageIO.getImageReadersByFormatName("png").next();try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(output))){reader.setInput(input);var metadata=reader.getImageMetadata(0);var root=metadata.getAsTree("javax_imageio_png_1.0");for(int i=0;i<root.getChildNodes().getLength();i++)assertFalse(Set.of("tEXt","iTXt","zTXt").contains(root.getChildNodes().item(i).getNodeName()));}finally{reader.dispose();}
    }
    @Test void unreadableSourceProducesOnlyGenericUnavailable(){var failure=assertThrows(DomainException.class,()->SafeMapImage.render(new byte[]{1,2,3},1,1,1,1,0,0,(x,y)->true));assertEquals(503,failure.status());assertEquals("ASSET_UNAVAILABLE",failure.code());assertEquals("Asset is unavailable",failure.getMessage());}
    @Test void renderingNeverUsesImageIoDiskCacheEvenWhenGlobalCachingIsEnabled()throws Exception{
        var original=encode(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB));boolean prior=ImageIO.getUseCache();var priorDirectory=ImageIO.getCacheDirectory();
        var directory=java.nio.file.Files.createTempDirectory("map-image-cache-probe-");
        try {
            ImageIO.setUseCache(true);ImageIO.setCacheDirectory(directory.toFile());java.nio.file.Files.delete(directory);
            assertDoesNotThrow(()->assertNotNull(decode(SafeMapImage.render(original,1,1,2,2,0,0,(x,y)->true))));
        }finally{ImageIO.setCacheDirectory(priorDirectory);ImageIO.setUseCache(prior);java.nio.file.Files.deleteIfExists(directory);}
    }
}
