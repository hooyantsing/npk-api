package xyz.hooy.npkapi.codec;

import xyz.hooy.npkapi.DdsSource;
import xyz.hooy.npkapi.DdsTable;
import xyz.hooy.npkapi.Frame;
import xyz.hooy.npkapi.NpkException;

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

    /**
     * ImageIO 是否具备 DDS 读取插件。插件注册在类加载期即固定，故缓存探测结果，
     * 避免每次 decode 都全量枚举 ImageIO 格式名（{@link ImageIO#getReaderFormatNames()}）。
     */
    private static final boolean DDS_SUPPORTED =
            Arrays.asList(ImageIO.getReaderFormatNames()).contains(DDS_FORMAT);

    /**
     * 将 FXT 帧解码为所引用 DDS 纹理的裁剪子图像。
     *
     * @param frame   FXT 帧描述（含 ddsIndex 与裁剪区域）
     * @param context 须携带 DdsTable 的解码上下文
     * @return 裁剪区域对应的子图像
     * @throws NpkException 缺少 DDS 插件、上下文缺少 DdsTable、DDS 源缺失，
     *                      或裁切出的子图尺寸非正/大于纹理尺寸时抛出
     */
    @Override
    public BufferedImage decode(Frame frame, CodecContext context) {
        BufferedImage ddsImage = getDdsImage(frame, context);

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

    private BufferedImage getDdsImage(Frame frame, CodecContext context) {
        DdsSource ddsSource = getDdsSource(frame, context);
        if (ddsSource == null || ddsSource.getRawData() == null) {
            throw new NpkException("DDS source not found for index " + frame.getDdsIndex());
        }

        BufferedImage ddsImage = ddsSource.getImage();
        if (ddsImage == null) {
            throw new NpkException("ImageIO returned null for DDS data. Unsupported DDS sub-format?");
        }
        return ddsImage;
    }

    private DdsSource getDdsSource(Frame frame, CodecContext context) {
        if (!isDdsSupported()) {
            throw new NpkException("ImageIO DDS plugin not available. "
                    + "Please ensure imageio-dds is on the classpath.");
        }

        // FXT 帧本身不持有像素数据，DDS 纹理来自上下文中的 DdsTable（已解压的完整 DDS）
        DdsTable ddsTable = context != null ? context.getDdsTable() : null;
        if (ddsTable == null) {
            throw new NpkException("DdsCodec requires a DdsTable in CodecContext.");
        }
        return ddsTable.get(frame.getDdsIndex());
    }

    /**
     * DDS 编码暂不支持 — 写入 DDS 纹理需要专门的压缩算法（如 S3TC/DXT）。
     *
     * @throws UnsupportedOperationException 恒定抛出；请通过 Version5Img.addDdsSource()
     *                                       登记预构建的 DDS 数据，再用 addDdsImage() 添加帧
     */
    @Override
    public Frame encode(BufferedImage image, CodecContext context) {
        throw new UnsupportedOperationException(
                "DDS encoding is not supported (no DXT compression). Use Version5Img.addDdsSource() "
                        + "to register pre-built DDS data, then Version5Img.addDdsImage() to add frames.");
    }

    /**
     * 检查当前 ImageIO 环境是否支持 DDS 格式读取（结果在类加载时探测并缓存）
     */
    public boolean isDdsSupported() {
        return DDS_SUPPORTED;
    }
}
