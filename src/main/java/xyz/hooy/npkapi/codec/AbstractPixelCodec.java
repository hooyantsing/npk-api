package xyz.hooy.npkapi.codec;

import xyz.hooy.npkapi.Frame;

import java.awt.image.BufferedImage;

/**
 * 像素编解码器骨架实现。
 * <p>
 * 为各具体编解码器提供通用的帧构建逻辑，
 * 子类只需实现 {@link PixelCodec#decode} 与 {@link PixelCodec#encode} 的像素转换部分。
 * </p>
 */
public abstract class AbstractPixelCodec implements PixelCodec {

    /**
     * 按图像帧通用模板构建帧：宽高与绘制偏移取自图像，
     * 帧域尺寸默认等于图像尺寸，像素数据按帧压缩标志压缩后写入。
     *
     * @param frameType 帧类型（如 {@link Frame#TYPE_ARGB8888}）
     * @param image     编码时的源图像
     * @param pixelData 编码后的像素字节
     * @return 已填充描述信息与像素数据的帧
     */
    protected Frame createFrame(int frameType, BufferedImage image, byte[] pixelData) {
        Frame frame = new Frame(frameType);
        frame.setWidth(image.getWidth());
        frame.setHeight(image.getHeight());
        frame.setX(image.getMinX());
        frame.setY(image.getMinY());
        frame.setFrameWidth(image.getWidth());
        frame.setFrameHeight(image.getHeight());
        frame.compressAndSetData(pixelData);
        return frame;
    }
}
