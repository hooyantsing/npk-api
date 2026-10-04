package xyz.hooy.npkit.codec;

import xyz.hooy.npkit.Bytes;
import xyz.hooy.npkit.Frame;

import java.awt.*;
import java.awt.image.*;
import java.util.Hashtable;

/**
 * ARGB 1555 像素编解码器。
 * <p>
 * 每像素 16 位：1 bit Alpha + 5 bit Red + 5 bit Green + 5 bit Blue。
 * Alpha 为 1 位透明度开关（全透明或全不透明）。
 * </p>
 */
public class Argb1555Codec implements PixelCodec {

    private static final int[] BITMASKS = {0x7C00, 0x03E0, 0x001F, 0x8000};

    @Override
    public BufferedImage decode(xyz.hooy.npkit.Frame frame, CodecContext context) {
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
    public xyz.hooy.npkit.Frame encode(BufferedImage image, CodecContext context) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] argbData = new int[width * height];
        image.getRGB(0, 0, width, height, argbData, 0, width);

        byte[] data = new byte[argbData.length * 2];
        int i = 0;
        for (int pixel : argbData) {
            int a = ((pixel >> 24) & 0xFF) > 0 ? 1 : 0;
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;
            r = r >> 3;
            g = g >> 3;
            b = b >> 3;
            data[i++] = (byte) (((g & 7) << 5) | b);
            // 16 位字布局：A(bit15) R(14:10) G(9:5) B(4:0)，小端高字节 = word 的 bit15~8，
            // 故 alpha 位于高字节 bit7，需左移 7 位
            data[i++] = (byte) ((a << 7) | (r << 2) | (g >> 3));
        }

        xyz.hooy.npkit.Frame frame = new xyz.hooy.npkit.Frame(xyz.hooy.npkit.Frame.TYPE_ARGB1555);
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
        return Frame.TYPE_ARGB1555;
    }
}
