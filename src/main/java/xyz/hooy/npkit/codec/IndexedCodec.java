package xyz.hooy.npkit.codec;

import xyz.hooy.npkit.Frame;
import xyz.hooy.npkit.NpkException;
import xyz.hooy.npkit.PaletteSource;

import java.awt.*;
import java.awt.image.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 索引色（调色板）像素编解码器。
 * <p>
 * 每像素 8 位索引值，通过 {@link PaletteSource} 映射到实际 RGBA 颜色。
 * 用于 IMG V4 和 V6 格式。
 * </p>
 */
public class IndexedCodec extends AbstractPixelCodec {

    @Override
    public BufferedImage decode(Frame frame, CodecContext context) {
        if (context == null || context.getPalette() == null) {
            throw new NpkException("IndexedCodec requires a Palette in CodecContext.");
        }
        PaletteSource palette = context.getPalette();
        byte[] data = frame.getDecompressedData();

        // 使用带 alpha 通道的字节数组构造器，保留调色板中每项的透明度（R, G, B, A）
        int size = palette.size();
        byte[] r = new byte[size];
        byte[] g = new byte[size];
        byte[] b = new byte[size];
        byte[] a = new byte[size];
        for (int i = 0; i < size; i++) {
            Color color = palette.get(i);
            r[i] = (byte) color.getRed();
            g[i] = (byte) color.getGreen();
            b[i] = (byte) color.getBlue();
            a[i] = (byte) color.getAlpha();
        }
        ColorModel colorModel = new IndexColorModel(8, size, r, g, b, a);
        SampleModel sampleModel = new PixelInterleavedSampleModel(
                DataBuffer.TYPE_BYTE, frame.getWidth(), frame.getHeight(),
                1, frame.getWidth(), new int[]{0});
        DataBuffer dataBuffer = new DataBufferByte(data, data.length);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        return new BufferedImage(colorModel, raster, false, null);
    }

    @Override
    public Frame encode(BufferedImage image, CodecContext context) {
        if (context == null || context.getPalette() == null) {
            throw new NpkException("IndexedCodec requires a Palette in CodecContext.");
        }
        PaletteSource palette = context.getPalette();
        int width = image.getWidth();
        int height = image.getHeight();
        int[] rgbData = new int[width * height];
        image.getRGB(0, 0, width, height, rgbData, 0, width);

        // ARGB → 索引哈希表：既有颜色复用索引，新颜色追加到调色板末尾
        Map<Integer, Integer> argbToIndex = new HashMap<>();
        for (int i = 0; i < palette.size(); i++) {
            argbToIndex.put(palette.get(i).getRGB(), i);
        }
        byte[] data = new byte[rgbData.length];
        for (int i = 0; i < rgbData.length; i++) {
            int argb = rgbData[i];
            Integer colorIndex = argbToIndex.get(argb);
            if (colorIndex == null) {
                palette.add(new Color(argb, true));
                colorIndex = palette.size() - 1;
                argbToIndex.put(argb, colorIndex);
            }
            data[i] = colorIndex.byteValue();
        }
        return createFrame(Frame.TYPE_INDEXED, image, data);
    }
}
