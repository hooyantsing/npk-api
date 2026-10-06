package xyz.hooy.npkapi.codec;

import xyz.hooy.npkapi.Frame;

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
public class Argb8888Codec extends AbstractPixelCodec {

    /**
     * 每像素字节数（32 位色）
     */
    private static final int BYTES_PER_PIXEL = 4;

    @Override
    public BufferedImage decode(Frame frame, CodecContext context) {
        return decodeRawPixels(
                frame.getDecompressedData(frame.expectedDataSize(BYTES_PER_PIXEL)),
                frame.getWidth(), frame.getHeight());
    }

    /**
     * 解码裸 BGRA 像素字节。返回图像直接包装入参数组（不复制），
     * 调用方此后不得修改该数组。
     */
    @Override
    public BufferedImage decodeRawPixels(byte[] pixelData, int width, int height) {
        ColorModel colorModel = new ComponentColorModel(
                ColorSpace.getInstance(ColorSpace.CS_sRGB),
                new int[]{8, 8, 8, 8}, true, false,
                ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        SampleModel sampleModel = new PixelInterleavedSampleModel(
                DataBuffer.TYPE_BYTE, width, height,
                // DNF 数据流按 B, G, R, A 顺序存储，而 ComponentColorModel 分量顺序为 R, G, B, A，
                // 故 band 偏移为 {2, 1, 0, 3}
                4, width * 4, new int[]{2, 1, 0, 3});
        DataBuffer dataBuffer = new DataBufferByte(pixelData, pixelData.length);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        return new BufferedImage(colorModel, raster, false, new Hashtable<>());
    }

    @Override
    public Frame encode(BufferedImage image, CodecContext context) {
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
        return createFrame(Frame.TYPE_ARGB8888, image, data);
    }
}
