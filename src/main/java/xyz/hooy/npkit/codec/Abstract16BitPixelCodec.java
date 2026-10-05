package xyz.hooy.npkit.codec;

import xyz.hooy.npkit.Bytes;
import xyz.hooy.npkit.Frame;

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
     * RGBA 分量位掩码，顺序为 {R, G, B, A}
     */
    protected abstract int[] bitMasks();

    @Override
    public BufferedImage decode(Frame frame, CodecContext context) {
        int[] masks = bitMasks();
        byte[] data = frame.getDecompressedData();
        ColorModel colorModel = new DirectColorModel(16, masks[0], masks[1], masks[2], masks[3]);
        SampleModel sampleModel = new SinglePixelPackedSampleModel(
                DataBuffer.TYPE_USHORT, frame.getWidth(), frame.getHeight(), masks);
        short[] shortData = Bytes.littleEndianShorts(data);
        DataBuffer dataBuffer = new DataBufferUShort(shortData, shortData.length);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        return new BufferedImage(colorModel, raster, false, new Hashtable<>());
    }
}
