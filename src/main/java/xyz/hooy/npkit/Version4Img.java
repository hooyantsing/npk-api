package xyz.hooy.npkit;

import xyz.hooy.npkit.codec.IndexedCodec;
import xyz.hooy.npkit.codec.PixelCodec;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * IMG V4 格式实现 — 索引色（调色板）图像。
 * <p>
 * V4 在 V2 的基础上增加了调色板支持。帧类型 0x0E 在 V4 上下文中
 * 表示索引色帧（而非 ARGB1555），每像素 8 位索引映射到调色板颜色。
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

    protected Palette palette;

    public Version4Img() {
        super();
        // V4 中 0x0E 表示索引色，注册 IndexedCodec 覆盖默认的 Argb1555Codec
        registerCodec(Frame.TYPE_INDEXED, new IndexedCodec());
    }

    @Override
    public int version() {
        return 4;
    }

    // ===== 调色板接口实现 =====

    @Override
    public int getPaletteCount() {
        return palette != null ? 1 : 0;
    }

    @Override
    public Palette getPalette(int index) {
        if (index != 0 || palette == null) {
            throw new IndexOutOfBoundsException("V4 IMG has at most 1 palette, index: " + index);
        }
        return palette;
    }

    @Override
    public Palette getActivePalette() {
        return palette;
    }

    // ===== 编解码 =====

    @Override
    protected BufferedImage decodeFrame(Frame frame) {
        // V4 中所有非引用帧都按索引色处理
        if (frame.isReference()) {
            return getImage(frame.getReference());
        }
        PixelCodec codec = getCodec(Frame.TYPE_INDEXED);
        return codec.decode(frame, createCodecContext());
    }

    @Override
    protected PixelCodec.CodecContext createCodecContext() {
        return new PixelCodec.CodecContext(palette);
    }

    // ===== Header 读写 =====

    @Override
    protected void readHeader(ImageInputStream stream) throws IOException {
        super.readHeader(stream);
        int colorNum = stream.readInt();
        byte[] paletteBytes = new byte[4 * colorNum];
        stream.readFully(paletteBytes);
        this.palette = Palette.fromBytes(paletteBytes, colorNum);
    }

    @Override
    protected void writeHeader(ImageOutputStream stream) throws IOException {
        super.writeHeader(stream);
        if (palette == null) {
            stream.writeInt(0);
        } else {
            stream.writeInt(palette.size());
            stream.write(palette.toBytes());
        }
    }
}
