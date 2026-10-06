package xyz.hooy.npkapi.codec;

import xyz.hooy.npkapi.DdsSource;
import xyz.hooy.npkapi.DdsTable;
import xyz.hooy.npkapi.Frame;
import xyz.hooy.npkapi.NpkException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * DDS 引用帧（V5 sprite 帧）编解码器。
 * <p>
 * 用于 IMG V5 中压缩状态为 {@link Frame#COMPRESSED_DDS} 的帧
 * （颜色系统可为 FXT1/FXT3/FXT5，也可为 ARGB8888 等裸像素格式）。
 * 这类帧不持有像素数据，其图像来自 DdsTable 中的共享纹理，
 * 解码时通过裁剪区域（leftCut, upCut, rightCut, downCut）获取子图像，
 * 并按旋转标志（{@link Frame#getRotate()}）还原图集中的旋转打包。
 * </p>
 * <p>
 * DXT 纹理的解码依赖 TwelveMonkeys imageio-dds 插件；裸像素纹理无此依赖。
 * 同一纹理的解码结果由 {@link DdsSource} 缓存（常被多个帧共享），
 * 本编解码器返回给调用方的子图像是独立拷贝，修改它不会污染缓存。
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
     * 将 DDS 引用帧解码为所引用纹理的裁剪子图像（独立拷贝）。
     * <p>
     * 若帧的旋转标志（{@link Frame#getRotate()}）为 1，说明该贴图在纹理图集中
     * 被旋转 90° 存放（图集打包优化），裁剪后须逆时针旋转 90° 还原
     * （与游戏行为一致，经真实文件与独立实现交叉验证）。
     * </p>
     *
     * @param frame   DDS 引用帧描述（含 ddsIndex、裁剪区域与旋转标志）
     * @param context 须携带 DdsTable 的解码上下文
     * @return 裁剪（并按需旋转）后的子图像
     * @throws NpkException 缺少 DDS 插件、上下文缺少 DdsTable、DDS 源缺失，
     *                      或裁切区域尺寸非正/超出纹理边界时抛出
     */
    @Override
    public BufferedImage decode(Frame frame, CodecContext context) {
        BufferedImage ddsImage = getDdsImage(frame, context);

        int cropWidth = frame.getRightCut() - frame.getLeftCut();
        int cropHeight = frame.getDownCut() - frame.getUpCut();
        if (cropWidth <= 0 || cropHeight <= 0) {
            throw new NpkException("Invalid DDS sub-image dimensions: " + cropWidth + "x" + cropHeight);
        }
        if (frame.getLeftCut() < 0 || frame.getUpCut() < 0
                || frame.getRightCut() > ddsImage.getWidth()
                || frame.getDownCut() > ddsImage.getHeight()) {
            throw new NpkException("DDS sub-image region [" + frame.getLeftCut() + ","
                    + frame.getUpCut() + "," + frame.getRightCut() + "," + frame.getDownCut()
                    + "] exceeds DDS source size ("
                    + ddsImage.getWidth() + "x" + ddsImage.getHeight() + ").");
        }

        boolean rotated = frame.getRotate() != 0;
        int outWidth = rotated ? cropHeight : cropWidth;
        int outHeight = rotated ? cropWidth : cropHeight;

        // DdsSource 缓存解码结果供多帧共享，getSubimage 与其共享栅格内存；
        // 逐像素拷贝为独立图像（按需逆时针旋转 90°），防止调用方修改污染缓存
        BufferedImage region = ddsImage.getSubimage(
                frame.getLeftCut(), frame.getUpCut(), cropWidth, cropHeight);
        BufferedImage copy = new BufferedImage(outWidth, outHeight, BufferedImage.TYPE_INT_ARGB);
        for (int sy = 0; sy < cropHeight; sy++) {
            for (int sx = 0; sx < cropWidth; sx++) {
                int argb = region.getRGB(sx, sy);
                if (rotated) {
                    // 逆时针 90°：src(x, y) → dst(y, cropWidth - 1 - x)
                    copy.setRGB(sy, cropWidth - 1 - sx, argb);
                } else {
                    copy.setRGB(sx, sy, argb);
                }
            }
        }
        return copy;
    }

    private BufferedImage getDdsImage(Frame frame, CodecContext context) {
        // DDS 引用帧本身不持有像素数据，纹理来自上下文中的 DdsTable（已解压的完整纹理数据）
        DdsTable ddsTable = context != null ? context.getDdsTable() : null;
        if (ddsTable == null) {
            throw new NpkException("DdsCodec requires a DdsTable in CodecContext.");
        }
        DdsSource ddsSource = ddsTable.get(frame.getDdsIndex());
        if (ddsSource == null) {
            throw new NpkException("DDS source not found for index " + frame.getDdsIndex());
        }
        // DXT 格式需要 ImageIO DDS 插件（DdsSource.getImage 内部校验）；裸像素格式无此依赖
        return ddsSource.getImage();
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
    public static boolean isDdsSupported() {
        return DDS_SUPPORTED;
    }
}
