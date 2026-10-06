package xyz.hooy.npkapi;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * DDS 纹理源数据模型。
 * <p>
 * IMG V5 的 DDS 纹理表（DdsTable）中的一张共享压缩纹理。
 * 多个 FXT 帧可通过索引引用同一张纹理的不同裁切区域。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * DdsSource dds = new DdsSource(ddsFileBytes, Frame.TYPE_FXT3, 256, 256);
 * int index = ((DdsCapableImg) img).addDdsSource(dds);
 * // 裁切纹理 (0,0)-(64,64) 区域，画布 64x64，绘制原点 (0,0)
 * ((DdsCapableImg) img).addDdsImage(index, new Rectangle(0, 0, 64, 64), new Rectangle(0, 0, 64, 64));
 * }</pre>
 */
public class DdsSource {

    private int title = 1;
    private int pixelFormat = Frame.TYPE_FXT1;
    private int index = -1;
    private int fullLength;
    private int length;
    private int width;
    private int height;
    private byte[] data;

    /**
     * 解码结果缓存：同一纹理常被多个 FXT 帧引用，惰性解码一次后复用。
     * setData 时失效。缓存图像为共享对象，调用方不应修改其像素。
     */
    private BufferedImage cachedImage;

    public DdsSource() {
    }

    /**
     * @param data        完整的 DDS 文件字节
     * @param pixelFormat DDS 像素压缩格式（如 {@link Frame#TYPE_FXT1}）
     * @param width       DDS 图像宽（一般要求被 4 整除）
     * @param height      DDS 图像高（一般要求被 4 整除）
     */
    public DdsSource(byte[] data, int pixelFormat, int width, int height) {
        this.data = data;
        this.pixelFormat = pixelFormat;
        this.width = width;
        this.height = height;
    }

    @Override
    public String toString() {
        return "DdsSource{index=" + index + ", " + width + "x" + height
                + ", pixelFormat=0x" + Integer.toHexString(pixelFormat) + "}";
    }

    /**
     * 解码为图像（依赖 ImageIO 的 DDS 插件）。
     * <p>
     * 解码结果被缓存：同一纹理常被多个 FXT 帧裁切引用，避免重复解码。
     * 返回的是共享缓存对象，调用方不应修改其像素；需要修改时请自行拷贝。
     * </p>
     *
     * @return 解码后的图像
     * @throws NpkException 无原始数据或解码失败
     */
    public BufferedImage getImage() {
        if (data == null) {
            throw new NpkException("DDS source at index " + index + " has no data to decode.");
        }
        if (cachedImage == null) {
            try {
                cachedImage = ImageIO.read(new ByteArrayInputStream(data));
            } catch (IOException e) {
                throw new NpkException("Failed to decode DDS source at index " + index, e);
            }
        }
        return cachedImage;
    }

    public int getTitle() {
        return title;
    }

    public void setTitle(int title) {
        this.title = title;
    }

    /**
     * DDS 像素压缩格式（0x12/0x13/0x14，即 FXT1/FXT3/FXT5）
     */
    public int getPixelFormat() {
        return pixelFormat;
    }

    public void setPixelFormat(int pixelFormat) {
        this.pixelFormat = pixelFormat;
    }

    /**
     * DDS 在纹理表中的索引（由 {@link Img#addDdsSource} 分配）
     */
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * 压缩前数据长度（字节），写出阶段按 ZLIB 压缩结果自动回填
     */
    public int getFullLength() {
        return fullLength;
    }

    public void setFullLength(int fullLength) {
        this.fullLength = fullLength;
    }

    /**
     * 压缩后数据长度（字节），写出阶段按 ZLIB 压缩结果自动回填
     */
    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * 完整的 DDS 文件字节
     */
    public byte[] getData() {
        return data;
    }

    /**
     * 设置完整的 DDS 文件字节，同时使解码缓存失效。
     */
    public void setData(byte[] data) {
        this.data = data;
        this.cachedImage = null;
    }
}
