package xyz.hooy.npkit.codec;

import xyz.hooy.npkit.DdsSource;
import xyz.hooy.npkit.DdsTable;
import xyz.hooy.npkit.Frame;
import xyz.hooy.npkit.NpkException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * DDS/FXT 纹理编解码器。
 * <p>
 * 用于 IMG V5 格式中的 FXT1/FXT3/FXT5 类型帧。
 * 这类帧的数据来源是 DdsTable 中的 DDS 纹理，
 * 解码时通过裁剪区域（leftCut, upCut, rightCut, downCut）获取子图像。
 * </p>
 * <p>
 * 依赖 TwelveMonkeys imageio-dds 插件进行 DDS 格式解码。
 * </p>
 */
public class DdsCodec implements PixelCodec {

    private static final String DDS_FORMAT = "DDS";

    @Override
    public BufferedImage decode(Frame frame, CodecContext context) {
        if (!isDdsSupported()) {
            throw new NpkException("ImageIO DDS plugin not available. "
                    + "Please ensure imageio-dds is on the classpath.");
        }

        // FXT 帧本身不持有像素数据，DDS 纹理来自上下文中的 DdsTable（已解压的完整 DDS）
        DdsTable ddsTable = context != null ? context.getDdsTable() : null;
        if (ddsTable == null) {
            throw new NpkException("DdsCodec requires a DdsTable in CodecContext.");
        }
        DdsSource ddsSource = ddsTable.get(frame.getDdsIndex());
        if (ddsSource == null || ddsSource.getRawData() == null) {
            throw new NpkException("DDS source not found for index " + frame.getDdsIndex());
        }

        BufferedImage ddsImage = ddsSource.getImage();
        if (ddsImage == null) {
            throw new NpkException("ImageIO returned null for DDS data. Unsupported DDS sub-format?");
        }

        int width = frame.getRightCut() - frame.getLeftCut();
        int height = frame.getDownCut() - frame.getUpCut();
        if (width <= 0 || height <= 0) {
            throw new NpkException("Invalid DDS sub-image dimensions: " + width + "x" + height);
        }
        if (width > ddsImage.getWidth() || height > ddsImage.getHeight()) {
            throw new NpkException("DDS sub-image region (" + width + "x" + height
                    + ") exceeds DDS source size (" + ddsImage.getWidth() + "x" + ddsImage.getHeight() + ").");
        }

        return ddsImage.getSubimage(frame.getLeftCut(), frame.getUpCut(), width, height);
    }

    @Override
    public Frame encode(BufferedImage image, CodecContext context) {
        // DDS 编码暂不支持 — 写入 DDS 纹理需要专门的压缩算法（如 S3TC/DXT）
        throw new UnsupportedOperationException(
                "DDS encoding is not supported (no DXT compression). Use Version5Img.addDdsSource() "
                        + "to register pre-built DDS data, then Version5Img.addDdsImage() to add frames.");
    }

    /**
     * 检查当前 ImageIO 环境是否支持 DDS 格式读取
     */
    public static boolean isDdsSupported() {
        return Arrays.asList(ImageIO.getReaderFormatNames()).contains(DDS_FORMAT);
    }
}
