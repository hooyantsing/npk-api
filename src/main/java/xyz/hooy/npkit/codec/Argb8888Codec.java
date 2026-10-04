package xyz.hooy.npkit.codec;

import xyz.hooy.npkit.Frame;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.util.Hashtable;

/**
 * ARGB 8888 像素编解码器。
 * <p>
 * 每像素 32 位：8 bit Blue + 8 bit Green + 8 bit Red + 8 bit Alpha（BGRA 字节序）。
 * </p>
 */
public class Argb8888Codec implements PixelCodec {

    @Override
    public BufferedImage decode(xyz.hooy.npkit.Frame frame, CodecContext context) {
        byte[] data = frame.getDecompressedData();
        ColorModel colorModel = new ComponentColorModel(
                ColorSpace.getInstance(ColorSpace.CS_sRGB),
                new int[]{8, 8, 8, 8}, true, false,
                ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        SampleModel sampleModel = new PixelInterleavedSampleModel(
                DataBuffer.TYPE_BYTE, frame.getWidth(), frame.getHeight(),
                // DNF 数据流按 B, G, R, A 顺序存储，而 ComponentColorModel 分量顺序为 R, G, B, A，
                // 故 band 偏移为 {2, 1, 0, 3}
                4, frame.getWidth() * 4, new int[]{2, 1, 0, 3});
        DataBuffer dataBuffer = new DataBufferByte(data, data.length);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        return new BufferedImage(colorModel, raster, false, new Hashtable<>());
    }

    @Override
    public xyz.hooy.npkit.Frame encode(BufferedImage image, CodecContext context) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] argbData = new int[width * height];
        image.getRGB(0, 0, width, height, argbData, 0, width);

        byte[] data = new byte[argbData.length * 4];
        int i = 0;
        for (int pixel : argbData) {
            data[i++] = (byte) (pixel & 0xFF);         // B
            data[i++] = (byte) ((pixel >> 8) & 0xFF);  // G
            data[i++] = (byte) ((pixel >> 16) & 0xFF); // R
            data[i++] = (byte) ((pixel >> 24) & 0xFF); // A
        }

        xyz.hooy.npkit.Frame frame = new xyz.hooy.npkit.Frame(xyz.hooy.npkit.Frame.TYPE_ARGB8888);
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
        return Frame.TYPE_ARGB8888;
    }
}
