package xyz.hooy.npkit;

import xyz.hooy.npkit.codec.IndexedCodec;
import xyz.hooy.npkit.codec.PixelCodec;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

/**
 * IMG V4 格式实现 — 索引色（调色板）图像。
 * <p>
 * V4 在 V2 的基础上增加了调色板支持。帧类型 0x0E 在 V4 上下文中
 * 表示索引色帧（而非 ARGB1555），每像素 8 位索引映射到调色板颜色；
 * 其余帧类型仍按 V2 语义分发（由构造器中的编解码器注册表决定）。
 * </p>
 *
 * <h3>V4 头部扩展结构</h3>
 * <pre>
 * [V2 Header]
 * [Palette]
 *   - colorCount (int32)
 *   - colors[colorCount] (4 bytes each: R, G, B, A)
 * </pre>
 */
public class Version4Img extends Version2Img {

    /**
     * 无状态索引色编解码器单例：所有 V4/V6 实例共享。
     */
    private static final PixelCodec INDEXED_CODEC = new IndexedCodec();

    protected PaletteSource palette;

    public Version4Img() {
        super();
        // V4 中 0x0E 表示索引色，注册 IndexedCodec 覆盖默认的 Argb1555Codec
        registerCodec(Frame.TYPE_INDEXED, INDEXED_CODEC);
        // 初始化一个空调色板，使新建的 V4 可直接 addImage（编码时自动追加颜色）；
        // 从文件读取时 readPalette 会覆盖此调色板
        this.palette = new PaletteSource();
    }

    @Override
    public int getVersion() {
        return 4;
    }

    @Override
    public boolean supportPaletteSource() {
        return true;
    }

    @Override
    public int getPaletteSourceCount() {
        return palette != null ? 1 : 0;
    }

    @Override
    public int getActivePaletteSourceIndex() {
        return 0;
    }

    @Override
    public void activatePaletteSource(int index) {
        if (index != 0 || palette == null) {
            throw new IndexOutOfBoundsException(
                    "V4 IMG has exactly 1 palette, index: " + index);
        }
    }

    @Override
    public PaletteSource getPaletteSource(int index) {
        if (index != 0 || palette == null) {
            throw new IndexOutOfBoundsException("V4 IMG has at most 1 palette, index: " + index);
        }
        return palette;
    }

    @Override
    protected PixelCodec.CodecContext createCodecContext() {
        return new PixelCodec.CodecContext(palette);
    }

    @Override
    protected void readHeader(ImageInputStream stream) throws IOException {
        super.readHeader(stream);
        readPalette(stream);
    }

    @Override
    protected void writeHeader(ImageOutputStream stream) throws IOException {
        super.writeHeader(stream);
        writePalette(stream);
    }

    /**
     * 读取调色板数据。子类（如 V6）可覆写以支持多调色板结构。
     */
    protected void readPalette(ImageInputStream stream) throws IOException {
        int colorNum = stream.readInt();
        byte[] paletteBytes = new byte[4 * colorNum];
        stream.readFully(paletteBytes);
        this.palette = PaletteSource.fromBytes(paletteBytes, colorNum);
    }

    /**
     * 写入调色板数据。子类（如 V6）可覆写以支持多调色板结构。
     */
    protected void writePalette(ImageOutputStream stream) throws IOException {
        if (palette == null) {
            stream.writeInt(0);
        } else {
            stream.writeInt(palette.size());
            stream.write(palette.toBytes());
        }
    }
}
