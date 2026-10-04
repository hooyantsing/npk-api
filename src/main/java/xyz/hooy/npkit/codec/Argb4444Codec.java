package xyz.hooy.npkit.codec;

import xyz.hooy.npkit.Bytes;
import xyz.hooy.npkit.Frame;

import java.awt.*;
import java.awt.image.*;
import java.util.Hashtable;

/**
 * ARGB 4444 像素编解码器。
 * <p>
 * 每像素 16 位：4 bit Alpha + 4 bit Red + 4 bit Green + 4 bit Blue。
 * </p>
 */
public class Argb4444Codec implements PixelCodec {

    private static final int[] BITMASKS = {0x0F00, 0x00F0, 0x000F, 0xF000};

    @Override
    public BufferedImage decode(Frame frame, CodecContext context) {
        byte[] data = frame.getDecompressedData();
        ColorModel colorModel = new DirectColorModel(16,
                BITMASKS[0], BITMASKS[1], BITMASKS[2], BITMASKS[3]);
        SampleModel sampleModel = new SinglePixelPackedSampleModel(
                DataBuffer.TYPE_USHORT, frame.getWidth(), frame.getHeight(), BITMASKS);
        short[] shortData = Bytes.shortsMergedFrom(data);
        DataBuffer dataBuffer = new DataBufferUShort(shortData, shortData.length);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        return new BufferedImage(colorModel, raster, false, new Hashtable<>());
    }

    @Override
    public Frame encode(BufferedImage image, CodecContext context) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] argbData = new int[width * height];
        image.getRGB(0, 0, width, height, argbData, 0, width);

        byte[] data = new byte[argbData.length * 2];
        int i = 0;
        for (int pixel : argbData) {
            int a = ((pixel >> 24) & 0xFF) >> 4;
            int r = ((pixel >> 16) & 0xFF) >> 4;
            int g = ((pixel >> 8) & 0xFF) >> 4;
            int b = (pixel & 0xFF) >> 4;
            data[i++] = (byte) ((g << 4) | b);
            data[i++] = (byte) ((a << 4) | r);
        }

        Frame frame = new Frame(Frame.TYPE_ARGB4444);
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
        return Frame.TYPE_ARGB4444;
    }
}
