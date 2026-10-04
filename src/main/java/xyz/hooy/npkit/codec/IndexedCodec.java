package xyz.hooy.npkit.codec;

import xyz.hooy.npkit.Frame;
import xyz.hooy.npkit.NpkException;
import xyz.hooy.npkit.Palette;

import java.awt.*;
import java.awt.image.*;

/**
 * 索引色（调色板）像素编解码器。
 * <p>
 * 每像素 8 位索引值，通过 {@link Palette} 映射到实际 RGBA 颜色。
 * 用于 IMG V4 和 V6 格式。
 * </p>
 */
public class IndexedCodec implements PixelCodec {

    @Override
    public BufferedImage decode(xyz.hooy.npkit.Frame frame, CodecContext context) {
        if (context == null || context.getPalette() == null) {
            throw new NpkException("IndexedCodec requires a Palette in CodecContext.");
        }
        Palette palette = context.getPalette();
        byte[] data = frame.getDecompressedData();

        ColorModel colorModel = new IndexColorModel(8, palette.size(),
                palette.toArray(), 0, false, -1, DataBuffer.TYPE_BYTE);
        SampleModel sampleModel = new PixelInterleavedSampleModel(
                DataBuffer.TYPE_BYTE, frame.getWidth(), frame.getHeight(),
                1, frame.getWidth(), new int[]{0});
        DataBuffer dataBuffer = new DataBufferByte(data, data.length);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        return new BufferedImage(colorModel, raster, false, null);
    }

    @Override
    public xyz.hooy.npkit.Frame encode(BufferedImage image, CodecContext context) {
        if (context == null || context.getPalette() == null) {
            throw new NpkException("IndexedCodec requires a Palette in CodecContext.");
        }
        Palette palette = context.getPalette();
        int width = image.getWidth();
        int height = image.getHeight();
        int[] rgbData = new int[width * height];
        image.getRGB(0, 0, width, height, rgbData, 0, width);

        byte[] data = new byte[rgbData.length];
        for (int i = 0; i < rgbData.length; i++) {
            Color color = new Color(rgbData[i], true);
            if (!palette.contains(color)) {
                palette.add(color);
            }
            data[i] = (byte) palette.indexOf(color);
        }

        xyz.hooy.npkit.Frame frame = new xyz.hooy.npkit.Frame(xyz.hooy.npkit.Frame.TYPE_INDEXED);
        frame.setWidth(width);
        frame.setHeight(height);
        frame.setX(image.getMinX());
        frame.setY(image.getMinY());
        frame.setFrameWidth(width);
        frame.setFrameHeight(height);
        frame.compressAndSetData(data);
        return frame;
    }

    @Override
    public int supportedFrameType() {
        return Frame.TYPE_INDEXED;
    }
}
