package xyz.hooy.npkapi;

import xyz.hooy.npkapi.codec.Argb1555Codec;
import xyz.hooy.npkapi.codec.Argb4444Codec;
import xyz.hooy.npkapi.codec.Argb8888Codec;
import xyz.hooy.npkapi.codec.DdsCodec;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * V5 纹理源数据模型（DDS 索引项）。
 * <p>
 * IMG V5 的纹理表（{@link DdsTable}）中的一张共享纹理（图集），
 * 多个 DDS 引用帧可通过索引引用同一张纹理的不同裁切区域。
 * </p>
 * <p>
 * 纹理数据按 {@code pixelFormat} 分两类存储（经真实游戏文件与多个独立实现交叉验证）：
 * </p>
 * <ul>
 *   <li><b>裸像素格式</b>（0x0E/0x0F/0x10，即 ARGB1555/ARGB4444/ARGB8888）：
 *       data 为未压缩的裸像素字节（大小 = width × height × 每像素字节数），无 DDS 文件头；</li>
 *   <li><b>DXT 压缩格式</b>（0x12/0x13/0x14，即 FXT1/FXT3/FXT5）：
 *       data 为完整的 DDS 文件字节（"DDS " 魔数 + 128 字节头 + DXT 数据），
 *       解码依赖 ImageIO 的 DDS 插件。</li>
 * </ul>
 * <p>
 * 纹理表项的两个大小字段依次为：<b>压缩后大小</b>（ZLIB 压缩后、IMG 数据区中的实际字节数）
 * 与<b>压缩前大小</b>（解压后的完整纹理数据字节数）。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * DdsSource dds = new DdsSource(ddsFileBytes, Frame.TYPE_FXT3, 256, 256);
 * int index = ((DdsCapable) img).addDdsSource(dds);
 * // 裁切纹理 (0,0)-(64,64) 区域，画布 64x64，绘制原点 (0,0)
 * ((DdsCapable) img).addDdsImage(index, new Rectangle(0, 0, 64, 64), new Rectangle(0, 0, 64, 64));
 * }</pre>
 */
public class DdsSource {

    /**
     * DDS 文件魔数 "DDS "
     */
    private static final byte[] DDS_FILE_MAGIC = {'D', 'D', 'S', ' '};

    /**
     * 裸像素格式解码器（无状态单例，供各 DdsSource 共享）
     */
    private static final Argb1555Codec ARGB1555_CODEC = new Argb1555Codec();
    private static final Argb4444Codec ARGB4444_CODEC = new Argb4444Codec();
    private static final Argb8888Codec ARGB8888_CODEC = new Argb8888Codec();

    private int title = 1;
    private int pixelFormat = Frame.TYPE_FXT1;
    private int index = -1;

    /**
     * 压缩前数据大小（字节）。写出阶段按 ZLIB 压缩结果自动回填。
     */
    private int rawSize;

    /**
     * 压缩后数据大小（字节，即 IMG 数据区中该纹理块的实际长度）。
     * 写出阶段按 ZLIB 压缩结果自动回填。
     */
    private int compressedSize;

    private int width;
    private int height;

    /**
     * 未压缩的完整纹理数据（DXT 格式为完整 DDS 文件字节，裸像素格式为像素字节）
     */
    private byte[] data;

    /**
     * 解码结果缓存：同一纹理常被多个帧引用，惰性解码一次后复用。
     * setData 时失效。缓存图像为共享对象，调用方不应修改其像素。
     */
    private BufferedImage cachedImage;

    public DdsSource() {
    }

    /**
     * @param data        完整纹理数据（DXT 格式为 DDS 文件字节；裸像素格式为像素字节）
     * @param pixelFormat 像素格式（{@link Frame#TYPE_FXT1} 等 DXT 格式，或
     *                    {@link Frame#TYPE_ARGB8888} 等裸像素格式）
     * @param width       纹理宽（DXT 格式一般要求被 4 整除）
     * @param height      纹理高（DXT 格式一般要求被 4 整除）
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
     * 该像素格式是否以裸像素（无 DDS 文件头）存储纹理数据
     */
    public static boolean isRawPixelFormat(int pixelFormat) {
        return Frame.isArgbType(pixelFormat);
    }

    /**
     * 该像素格式是否以完整 DDS 文件（DXT 压缩）存储纹理数据
     */
    public static boolean isDxtPixelFormat(int pixelFormat) {
        return Frame.isFxtType(pixelFormat);
    }

    /**
     * 裸像素格式下每像素字节数（ARGB8888 为 4，ARGB1555/ARGB4444 为 2）
     *
     * @throws NpkException 非裸像素格式
     */
    public static int rawBytesPerPixel(int pixelFormat) {
        if (pixelFormat == Frame.TYPE_ARGB8888) {
            return 4;
        }
        if (pixelFormat == Frame.TYPE_ARGB1555 || pixelFormat == Frame.TYPE_ARGB4444) {
            return 2;
        }
        throw new NpkException("Not a raw pixel format: 0x" + Integer.toHexString(pixelFormat));
    }

    /**
     * 校验纹理数据与声明的像素格式/宽高一致（登记或解码前调用，尽早给出明确错误）。
     *
     * @throws NpkException 数据为 null、像素格式未知、裸像素数据长度与宽高不符，
     *                      或 DXT 数据缺少 "DDS " 魔数
     */
    public void validateData() {
        if (data == null || data.length == 0) {
            throw new NpkException("Texture source at index " + index + " has no data.");
        }
        if (isRawPixelFormat(pixelFormat)) {
            long expected = (long) width * height * rawBytesPerPixel(pixelFormat);
            if (width < 0 || height < 0 || data.length != expected) {
                throw new NpkException("Raw texture data size " + data.length
                        + " does not match " + width + "x" + height + " at "
                        + rawBytesPerPixel(pixelFormat) + " bytes/pixel (expected "
                        + expected + ") for texture at index " + index + ".");
            }
        } else if (isDxtPixelFormat(pixelFormat)) {
            if (data.length < DDS_FILE_MAGIC.length
                    || data[0] != DDS_FILE_MAGIC[0] || data[1] != DDS_FILE_MAGIC[1]
                    || data[2] != DDS_FILE_MAGIC[2] || data[3] != DDS_FILE_MAGIC[3]) {
                throw new NpkException("DXT texture at index " + index
                        + " is not a DDS file (missing 'DDS ' magic); pixelFormat=0x"
                        + Integer.toHexString(pixelFormat)
                        + " requires complete DDS file bytes.");
            }
        } else {
            throw new NpkException("Unsupported texture pixel format 0x"
                    + Integer.toHexString(pixelFormat) + " at index " + index
                    + "; expected 0x0E/0x0F/0x10 (raw) or 0x12/0x13/0x14 (DXT).");
        }
    }

    /**
     * 解码为图像。
     * <p>
     * 裸像素格式经对应编解码器直接解码；DXT 格式按完整 DDS 文件交由 ImageIO
     * （依赖 TwelveMonkeys imageio-dds 插件，见 {@link DdsCodec#isDdsSupported()}）。
     * 解码结果被缓存：同一纹理常被多个帧裁切引用，避免重复解码。
     * 返回的是共享缓存对象，调用方不应修改其像素；需要修改时请自行拷贝。
     * </p>
     *
     * @return 解码后的图像
     * @throws NpkException 无数据、数据与声明不符或解码失败
     */
    public BufferedImage getImage() {
        if (cachedImage == null) {
            validateData();
            if (isRawPixelFormat(pixelFormat)) {
                switch (pixelFormat) {
                    case Frame.TYPE_ARGB8888:
                        cachedImage = ARGB8888_CODEC.decodeRawPixels(data, width, height);
                        break;
                    case Frame.TYPE_ARGB4444:
                        cachedImage = ARGB4444_CODEC.decodeRawPixels(data, width, height);
                        break;
                    default: // TYPE_ARGB1555（V5 裸像素语境下 0x0E 为 ARGB1555）
                        cachedImage = ARGB1555_CODEC.decodeRawPixels(data, width, height);
                        break;
                }
            } else {
                if (!DdsCodec.isDdsSupported()) {
                    throw new NpkException("ImageIO DDS plugin not available. "
                            + "Please ensure imageio-dds is on the classpath.");
                }
                try {
                    cachedImage = ImageIO.read(new ByteArrayInputStream(data));
                } catch (IOException e) {
                    throw new NpkException("Failed to decode DDS texture at index " + index, e);
                }
                if (cachedImage == null) {
                    throw new NpkException("ImageIO returned null for DDS data at index " + index
                            + ". Unsupported DDS sub-format?");
                }
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
     * 像素格式（0x12/0x13/0x14 即 FXT1/FXT3/FXT5，或 0x0E/0x0F/0x10 裸像素格式）
     */
    public int getPixelFormat() {
        return pixelFormat;
    }

    public void setPixelFormat(int pixelFormat) {
        this.pixelFormat = pixelFormat;
    }

    /**
     * 纹理在表中的索引（由 {@link DdsCapable#addDdsSource} 分配）
     */
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * 压缩前数据大小（字节），写出阶段按 ZLIB 压缩结果自动回填
     */
    public int getRawSize() {
        return rawSize;
    }

    public void setRawSize(int rawSize) {
        this.rawSize = rawSize;
    }

    /**
     * 压缩后数据大小（字节），写出阶段按 ZLIB 压缩结果自动回填
     */
    public int getCompressedSize() {
        return compressedSize;
    }

    public void setCompressedSize(int compressedSize) {
        this.compressedSize = compressedSize;
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
     * 未压缩的完整纹理数据。
     * 注意：返回内部引用，调用方不应修改其内容（解码缓存可能直接包装该数组）。
     */
    public byte[] getData() {
        return data;
    }

    /**
     * 设置未压缩的完整纹理数据，同时使解码缓存失效。
     */
    public void setData(byte[] data) {
        this.data = data;
        this.cachedImage = null;
    }
}
