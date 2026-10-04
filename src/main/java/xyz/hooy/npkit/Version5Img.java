package xyz.hooy.npkit;

import xyz.hooy.npkit.codec.DdsCodec;
import xyz.hooy.npkit.codec.PixelCodec;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * IMG V5 格式实现 — 支持 DDS 纹理的图像。
 * <p>
 * V5 在 V2 的基础上增加了 DDS 纹理表（DdsTable）。
 * FXT1/FXT2/FXT3 类型的帧不直接存储像素数据，
 * 而是引用 DDS 表中的共享纹理，并通过裁剪区域获取子图像。
 * </p>
 *
 * <h3>V5 头部扩展结构</h3>
 * <pre>
 * [V2 Header]
 * [DDS Table]
 *   - ddsCount (int32)
 *   - imgLength (int32, unused)
 *   - colorNum (int32, unused in V5 but present)
 *   - palette[colorNum] (4 bytes each, unused)
 *   - dds[ddsCount]:
 *     - title (int32)
 *     - pixelFormat (int32)
 *     - index (int32)
 *     - fullLength (int32)
 *     - length (int32)
 *     - width (int32)
 *     - height (int32)
 * </pre>
 *
 * <h3>V5 帧表扩展</h3>
 * <p>
 * FXT 类型帧在标准帧描述后附加 DDS 裁剪信息：
 * ddsIndex, leftCut, upCut, rightCut, downCut
 * </p>
 */
public class Version5Img extends Version2Img {

    protected DdsTable ddsTable;

    /**
     * 写入阶段缓存的 DDS 压缩数据（index → ZLIB 压缩后字节），
     * 保证头部写入的 length 字段与实际写入的数据长度一致。
     */
    private final Map<Integer, byte[]> ddsCompressedCache = new LinkedHashMap<>();

    /**
     * V5 头部中的「整个 IMG 文件大小」字段。该字段对解析无实际用途，
     * 读取时保存以便重新序列化时原样写回，保证往返保真。
     */
    private int imgLength;

    /**
     * V5 头部中类似 IMG V4 的调色板数据。在 V5 中没有任何用途，
     * 绝大部分 V5 文件该调色板颜色项数为 0（退化为一个全零双字），
     * 但极少数文件（如 sprite_map_infernomode.NPK）仍保留有颜色项。
     * 读取时保存并在写回时还原，保证往返保真。
     */
    private Palette headerPalette = new Palette();

    public Version5Img() {
        super();
        // 注册 DDS 编解码器
        DdsCodec ddsCodec = new DdsCodec();
        registerCodec(Frame.TYPE_FXT1, ddsCodec);
        registerCodec(Frame.TYPE_FXT2, ddsCodec);
        registerCodec(Frame.TYPE_FXT3, ddsCodec);
    }

    @Override
    public int version() {
        return 5;
    }

    // ===== DDS 接口实现 =====

    @Override
    public int getDdsSourceCount() {
        return ddsTable != null ? ddsTable.size() : 0;
    }

    @Override
    public BufferedImage getDdsSource(int index) {
        if (ddsTable == null) {
            return null;
        }
        DdsTable.DDS dds = ddsTable.get(index);
        if (dds == null || dds.getRawData() == null) {
            return null;
        }
        try {
            return ImageIO.read(new ByteArrayInputStream(dds.getRawData()));
        } catch (IOException e) {
            throw new NpkException("Failed to decode DDS source at index " + index, e);
        }
    }

    @Override
    public DdsTable getDdsTable() {
        return ddsTable;
    }

    /**
     * 获取 V5 头部中的「整个 IMG 文件大小」字段（读取时保存，对解析无实际用途）。
     */
    public int getImgLength() {
        return imgLength;
    }

    /**
     * 获取 V5 头部调色板（在 V5 中无用途，仅为往返保真而保留）。
     */
    public Palette getHeaderPalette() {
        return headerPalette;
    }

    /**
     * 设置 V5 头部调色板（在 V5 中无用途，仅为往返保真而保留）。
     */
    public void setHeaderPalette(Palette headerPalette) {
        this.headerPalette = headerPalette != null ? headerPalette : new Palette();
    }

    // ===== Header 读写 =====

    @Override
    protected void readHeader(ImageInputStream stream) throws IOException {
        super.readHeader(stream);

        int ddsCount = stream.readInt();
        this.imgLength = stream.readInt(); // 整个 IMG 文件大小（V5 中无用途，保存以往返写回）
        int colorNum = stream.readInt();   // V5 头部调色板颜色数目（绝大部分为 0）
        if (colorNum > 0) {
            byte[] paletteBytes = new byte[4 * colorNum];
            stream.readFully(paletteBytes);
            this.headerPalette = Palette.fromBytes(paletteBytes, colorNum);
        } else {
            this.headerPalette = new Palette();
        }

        DdsTable table = new DdsTable();
        for (int i = 0; i < ddsCount; i++) {
            DdsTable.DDS dds = new DdsTable.DDS();
            dds.setTitle(stream.readInt());
            dds.setPixelFormat(stream.readInt());
            dds.setIndex(stream.readInt());
            dds.setFullLength(stream.readInt());
            dds.setLength(stream.readInt());
            dds.setWidth(stream.readInt());
            dds.setHeight(stream.readInt());
            table.put(dds);
        }
        this.ddsTable = table;
    }

    @Override
    protected void writeHeader(ImageOutputStream stream) throws IOException {
        super.writeHeader(stream);

        int ddsCount = ddsTable != null ? ddsTable.size() : 0;
        stream.writeInt(ddsCount);
        stream.writeInt(imgLength); // 还原读取到的「整个 IMG 文件大小」字段
        // V5 头部调色板：在 V5 中无用途，但极少数文件保留有颜色项，需往返保真
        if (headerPalette != null && headerPalette.size() > 0) {
            stream.writeInt(headerPalette.size());
            stream.write(headerPalette.toBytes());
        } else {
            stream.writeInt(0);
        }

        if (ddsTable != null) {
            // DDS 数据以 ZLIB 压缩形式存储：先压缩以确定头部的大小字段，并缓存供写数据阶段使用
            ddsCompressedCache.clear();
            for (DdsTable.DDS dds : ddsTable.values()) {
                if (dds.getRawData() != null) {
                    byte[] compressed = Bytes.compress(dds.getRawData());
                    dds.setFullLength(dds.getRawData().length);
                    dds.setLength(compressed.length);
                    ddsCompressedCache.put(dds.getIndex(), compressed);
                }
                stream.writeInt(dds.getTitle());
                stream.writeInt(dds.getPixelFormat());
                stream.writeInt(dds.getIndex());
                stream.writeInt(dds.getFullLength());
                stream.writeInt(dds.getLength());
                stream.writeInt(dds.getWidth());
                stream.writeInt(dds.getHeight());
            }
        }
    }

    // ===== Frame 读写 =====

    @Override
    protected void readFrames(ImageInputStream stream) throws IOException {
        List<Frame> frameList = new ArrayList<>(frameSize);
        for (int i = 0; i < frameSize; i++) {
            Frame frame = new Frame();
            frame.setType(stream.readInt());
            readFrame(stream, frame);
            frameList.add(frame);
        }

        // 先读 DDS 纹理数据：文件中存储的是 ZLIB 压缩后的 DDS，
        // 按「压缩后大小(length)」读取字节并解压得到完整 DDS 文件
        if (ddsTable != null) {
            for (DdsTable.DDS dds : ddsTable.values()) {
                byte[] compressed = new byte[dds.getLength()];
                stream.readFully(compressed);
                dds.setRawData(Bytes.decompress(compressed));
            }
        }

        // 再读非 DDS 帧的像素数据
        for (int i = 0; i < frameSize; i++) {
            Frame frame = frameList.get(i);
            if (!frame.isReference() && !frame.isFxt()) {
                readFrameRawData(stream, frame);
            }
        }

        // FXT 帧不持有自身像素数据，其 DDS 纹理由 DdsCodec 从 ddsTable 按 ddsIndex 获取，
        // 此处不再挂载 rawData，以免 setRawData 覆盖保留的 length 字段

        this.frames = frameList;
    }

    @Override
    protected void readFrame(ImageInputStream stream, Frame frame) throws IOException {
        if (frame.isFxt()) {
            // FXT 帧：标准图像帧描述 + DDS 裁剪信息
            frame.setCompressed(stream.readInt());
            frame.setWidth(stream.readInt());
            frame.setHeight(stream.readInt());
            frame.setLength(stream.readInt());
            frame.setX(stream.readInt());
            frame.setY(stream.readInt());
            frame.setFrameWidth(stream.readInt());
            frame.setFrameHeight(stream.readInt());
            stream.skipBytes(4); // padding
            frame.setDdsIndex(stream.readInt());
            frame.setLeftCut(stream.readInt());
            frame.setUpCut(stream.readInt());
            frame.setRightCut(stream.readInt());
            frame.setDownCut(stream.readInt());
            stream.skipBytes(4); // padding
        } else if (frame.isReference()) {
            frame.setReference(stream.readInt());
        } else {
            super.readFrame(stream, frame);
        }
    }

    @Override
    protected void writeFrames(ImageOutputStream stream) throws IOException {
        // 写帧表
        for (Frame frame : frames) {
            writeFrame(stream, frame);
        }
        // 写 DDS 数据（ZLIB 压缩后的字节，与头部 length 字段一致）
        if (ddsTable != null) {
            for (DdsTable.DDS dds : ddsTable.values()) {
                byte[] compressed = ddsCompressedCache.get(dds.getIndex());
                if (compressed != null) {
                    stream.write(compressed);
                } else if (dds.getRawData() != null) {
                    stream.write(dds.getRawData());
                }
            }
        }
        // 写非 FXT、非引用帧的像素数据
        for (Frame frame : frames) {
            if (!frame.isReference() && !frame.isFxt()) {
                writeFrameRawData(stream, frame);
            }
        }
    }

    @Override
    protected void writeFrame(ImageOutputStream stream, Frame frame) throws IOException {
        stream.writeInt(frame.getType());
        if (frame.isFxt()) {
            stream.writeInt(frame.getCompressed());
            stream.writeInt(frame.getWidth());
            stream.writeInt(frame.getHeight());
            stream.writeInt(frame.getLength());
            stream.writeInt(frame.getX());
            stream.writeInt(frame.getY());
            stream.writeInt(frame.getFrameWidth());
            stream.writeInt(frame.getFrameHeight());
            stream.writeInt(0); // padding
            stream.writeInt(frame.getDdsIndex());
            stream.writeInt(frame.getLeftCut());
            stream.writeInt(frame.getUpCut());
            stream.writeInt(frame.getRightCut());
            stream.writeInt(frame.getDownCut());
            stream.writeInt(0); // padding
        } else if (frame.isReference()) {
            stream.writeInt(frame.getReference());
        } else {
            // 标准 ARGB 帧
            stream.writeInt(frame.getCompressed());
            stream.writeInt(frame.getWidth());
            stream.writeInt(frame.getHeight());
            stream.writeInt(frame.getLength());
            stream.writeInt(frame.getX());
            stream.writeInt(frame.getY());
            stream.writeInt(frame.getFrameWidth());
            stream.writeInt(frame.getFrameHeight());
        }
    }

    // ===== 编解码 =====

    @Override
    protected BufferedImage decodeFrame(Frame frame) {
        if (frame.isReference()) {
            return getImage(frame.getReference());
        }
        if (frame.isFxt()) {
            PixelCodec codec = getCodec(frame.getType());
            return codec.decode(frame, createCodecContext());
        }
        // 非 FXT 帧按 V2 方式处理
        return super.decodeFrame(frame);
    }

    @Override
    protected PixelCodec.CodecContext createCodecContext() {
        return new PixelCodec.CodecContext(null, ddsTable);
    }

    @Override
    protected int calculateTableLength() {
        int length = 0;
        for (Frame frame : frames) {
            length += 4; // type
            if (frame.isFxt()) {
                length += 60; // 15 int fields (including 2 paddings)
            } else if (frame.isReference()) {
                length += 4;
            } else {
                length += 32;
            }
        }
        return length;
    }
}
