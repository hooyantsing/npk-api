package xyz.hooy.npkapi.codec;

import xyz.hooy.npkapi.Bytes;
import xyz.hooy.npkapi.Frame;

import java.awt.*;
import java.awt.image.*;
import java.util.Hashtable;

/**
 * 16 位打包像素编解码器骨架实现（如 ARGB1555、ARGB4444）。
 * <p>
 * 像素以 16 位字（小端序存储）打包，各分量通过位掩码切分，
 * 子类只需声明自身的位掩码布局与编码逻辑。
 * </p>
 */
public abstract class Abstract16BitPixelCodec extends AbstractPixelCodec {

    /**
     * 每像素字节数（16 位色）
     */
    private static final int BYTES_PER_PIXEL = 2;

    /**
     * RGBA 分量位掩码，顺序为 {R, G, B, A}
     */
    protected abstract int[] bitMasks();

    @Override
    public BufferedImage decode(Frame frame, CodecContext context) {
        return decodeRawPixels(
                frame.getDecompressedData(frame.expectedDataSize(BYTES_PER_PIXEL)),
                frame.getWidth(), frame.getHeight());
    }

    @Override
    public BufferedImage decodeRawPixels(byte[] pixelData, int width, int height) {
        int[] masks = bitMasks();
        ColorModel colorModel = new DirectColorModel(16, masks[0], masks[1], masks[2], masks[3]);
        SampleModel sampleModel = new SinglePixelPackedSampleModel(
                DataBuffer.TYPE_USHORT, width, height, masks);
        short[] shortData = Bytes.littleEndianShorts(pixelData);
        DataBuffer dataBuffer = new DataBufferUShort(shortData, shortData.length);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        return new BufferedImage(colorModel, raster, false, new Hashtable<>());
    }
}
