package io.dndroll.domain;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.function.BiPredicate;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/** Fresh RGB encoding only: source metadata, alpha and covered samples never leave here. */
public final class SafeMapImage {
    private SafeMapImage() {}
    public static byte[] render(byte[] original, int width, int height, int logicalGrid, int sourceGrid,
                                int offsetX, int offsetY, BiPredicate<Integer,Integer> revealed) {
        try {
            BufferedImage source;
            try(var input=new MemoryCacheImageInputStream(new ByteArrayInputStream(original))) {
                var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw unavailable();var reader=readers.next();
                try{reader.setInput(input,true,true);source=reader.read(0);}finally{reader.dispose();}
            }
            if(source==null) throw unavailable();
            var output=new BufferedImage(Math.multiplyExact(width,logicalGrid),Math.multiplyExact(height,logicalGrid),BufferedImage.TYPE_INT_RGB);
            for(int y=0;y<output.getHeight();y++) for(int x=0;x<output.getWidth();x++) {
                if(!revealed.test(x/logicalGrid,y/logicalGrid)) continue;
                int sx=offsetX+(int)((long)x*sourceGrid/logicalGrid),sy=offsetY+(int)((long)y*sourceGrid/logicalGrid);
                if(sx<0||sy<0||sx>=source.getWidth()||sy>=source.getHeight()) continue;
                int rgba=source.getRGB(sx,sy),alpha=rgba>>>24;
                int red=((rgba>>>16)&255)*alpha/255,green=((rgba>>>8)&255)*alpha/255,blue=(rgba&255)*alpha/255;
                output.setRGB(x,y,(red<<16)|(green<<8)|blue);
            }
            var bytes=new ByteArrayOutputStream();
            try(var stream=new MemoryCacheImageOutputStream(bytes)) {if(!ImageIO.write(output,"png",stream)) throw unavailable();}
            return bytes.toByteArray();
        } catch(IOException failure) { throw unavailable(); }
    }
    private static DomainException unavailable() { return new DomainException("ASSET_UNAVAILABLE",503,"Asset is unavailable"); }
}
