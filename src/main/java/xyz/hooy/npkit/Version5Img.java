package xyz.hooy.npkit;

import xyz.hooy.npkit.codec.DdsCodec;
import xyz.hooy.npkit.codec.PixelCodec;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * IMG V5 格式实现 — 支持 DDS 纹理的图像。
 * <p>
 * V5 在 V2 的基础上增加了 DDS 纹理表（DdsTable）。
 * FXT1/FXT3/FXT5 类型的帧不直接存储像素数据，
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
public class Version5Img extends AbstractImg {

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
    private PaletteSource headerPalette = new PaletteSource();

    public Version5Img() {
        super();
        // 注册 DDS 编解码器
        DdsCodec ddsCodec = new DdsCodec();
        registerCodec(Frame.TYPE_FXT1, ddsCodec);
        registerCodec(Frame.TYPE_FXT3, ddsCodec);
        registerCodec(Frame.TYPE_FXT5, ddsCodec);
    }

    @Override
    public int getVersion() {
        return 5;
    }

    @Override
    public boolean supportDdsSource() {
        return true;
    }

    @Override
    public int getDdsSourceCount() {
        return ddsTable != null ? ddsTable.size() : 0;
    }

    @Override
    public DdsSource getDdsSource(int index) {
        return ddsTable != null ? ddsTable.get(index) : null;
    }

    @Override
    protected void readHeader(ImageInputStream stream) throws IOException {
        super.readHeader(stream);

        int ddsCount = stream.readInt();
        this.imgLength = stream.readInt(); // 整个 IMG 文件大小（V5 中无用途，保存以往返写回）
        int colorNum = stream.readInt();   // V5 头部调色板颜色数目（绝大部分为 0）
        if (colorNum > 0) {
            byte[] paletteBytes = new byte[4 * colorNum];
            stream.readFully(paletteBytes);
            this.headerPalette = PaletteSource.fromBytes(paletteBytes, colorNum);
        } else {
            this.headerPalette = new PaletteSource();
        }

        DdsTable table = new DdsTable();
        for (int i = 0; i < ddsCount; i++) {
            DdsSource ddsSource = new DdsSource();
            ddsSource.setTitle(stream.readInt());
            ddsSource.setPixelFormat(stream.readInt());
            ddsSource.setIndex(stream.readInt());
            ddsSource.setFullLength(stream.readInt());
            ddsSource.setLength(stream.readInt());
            ddsSource.setWidth(stream.readInt());
            ddsSource.setHeight(stream.readInt());
            table.put(ddsSource);
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
            for (DdsSource ddsSource : ddsTable.values()) {
                if (ddsSource.getRawData() != null) {
                    byte[] compressed = Bytes.compress(ddsSource.getRawData());
                    ddsSource.setFullLength(ddsSource.getRawData().length);
                    ddsSource.setLength(compressed.length);
                    ddsCompressedCache.put(ddsSource.getIndex(), compressed);
                }
                stream.writeInt(ddsSource.getTitle());
                stream.writeInt(ddsSource.getPixelFormat());
                stream.writeInt(ddsSource.getIndex());
                stream.writeInt(ddsSource.getFullLength());
                stream.writeInt(ddsSource.getLength());
                stream.writeInt(ddsSource.getWidth());
                stream.writeInt(ddsSource.getHeight());
            }
        }
    }

    @Override
    protected void readFrames(ImageInputStream stream) throws IOException {
        List<Frame> frameList = new ArrayList<>(frameSize);
        for (int i = 0; i < frameSize; i++) {
            Frame frame = new Frame();
            frame.setType(stream.readInt());
            readFrameDescriptor(stream, frame);
            frameList.add(frame);
        }

        // 先读 DDS 纹理数据：文件中存储的是 ZLIB 压缩后的 DDS，
        // 按「压缩后大小(length)」读取字节并解压得到完整 DDS 文件
        if (ddsTable != null) {
            for (DdsSource ddsSource : ddsTable.values()) {
                byte[] compressed = new byte[ddsSource.getLength()];
                stream.readFully(compressed);
                ddsSource.setRawData(Bytes.decompress(compressed));
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
    protected void readFrameDescriptor(ImageInputStream stream, Frame frame) throws IOException {
        if (frame.isFxt()) {
            // FXT 帧：标准图像帧描述 + DDS 裁剪信息
            readStandardFrameFields(stream, frame);
            stream.skipBytes(4); // padding
            frame.setDdsIndex(stream.readInt());
            frame.setLeftCut(stream.readInt());
            frame.setUpCut(stream.readInt());
            frame.setRightCut(stream.readInt());
            frame.setDownCut(stream.readInt());
            stream.skipBytes(4); // padding
        } else {
            super.readFrameDescriptor(stream, frame);
        }
    }

    @Override
    protected void writeFrames(ImageOutputStream stream) throws IOException {
        // 写帧表
        for (Frame frame : frames) {
            writeFrameDescriptor(stream, frame);
        }
        // 写 DDS 数据（ZLIB 压缩后的字节，与头部 length 字段一致）
        if (ddsTable != null) {
            for (DdsSource ddsSource : ddsTable.values()) {
                byte[] compressed = ddsCompressedCache.get(ddsSource.getIndex());
                if (compressed != null) {
                    stream.write(compressed);
                } else if (ddsSource.getRawData() != null) {
                    stream.write(ddsSource.getRawData());
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
    protected void writeFrameDescriptor(ImageOutputStream stream, Frame frame) throws IOException {
        // 帧 type 须先于描述体写入，与 readFrames「先 setType(readInt) 再 readFrameDescriptor」对称；
        // 若遗漏则每个 FXT 帧少写 4 字节，重读时字节错位、DDS 数据解压失败。
        stream.writeInt(frame.getType());
        if (frame.isFxt()) {
            writeStandardFrameFields(stream, frame);
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
            writeStandardFrameFields(stream, frame);
        }
    }

    @Override
    protected PixelCodec.CodecContext createCodecContext() {
        return new PixelCodec.CodecContext(ddsTable);
    }

    @Override
    protected int frameTableEntrySize(Frame frame) {
        if (frame.isFxt()) {
            return 64; // type(4) + 8 个 int 字段(32) + padding(4) + 裁剪信息(20) + padding(4)
        }
        return super.frameTableEntrySize(frame);
    }

    /**
     * 登记一张 DDS 纹理到 DDS 表，返回其分配的索引。
     * <p>
     * 传入的 {@link DdsSource} 须携带完整的 DDS 文件字节
     * （本库不实现 BufferedImage → DDS 的 DXT 压缩）。
     * 压缩前/后大小（fullLength/length）会在 writeHeader 阶段按 ZLIB 压缩结果自动回填。
     * </p>
     *
     * @param ddsSource 携带完整 DDS 文件字节与像素格式、宽高的纹理源
     * @return 该 DDS 在表中分配的索引，供 {@link #addDdsImage} 引用
     */
    @Override
    public int addDdsSource(DdsSource ddsSource) {
        if (ddsSource == null || ddsSource.getRawData() == null || ddsSource.getRawData().length == 0) {
            throw new NpkException("DDS data cannot be null or empty.");
        }
        if (ddsTable == null) {
            ddsTable = new DdsTable();
        }
        ddsTable.put(ddsSource); // index < 0 时自动分配下一个可用索引
        invalidateRawData();
        return ddsSource.getIndex();
    }

    /**
     * 追加一个引用 DDS 纹理裁切区域的 FXT 帧（DDS 引用型索引项）。
     * <p>
     * 帧的颜色系统（type）无需显式传入——一帧即从所引用的 DDS 大图裁切而来，
     * 其压缩方式必然等于该 DDS 的像素格式，故直接取自 {@code DdsSource.pixelFormat}
     * （取值 0x12/0x13/0x14，与 {@link Frame#TYPE_FXT1} 等同一编码空间）。
     * 帧的贴图宽高由裁切区域自动得出（right-left × down-up），
     * 压缩状态按格式规范固定为 7（ZLIB），保留的 length 字段置 0。
     * </p>
     *
     * @param ddsSourceIndex 引用的 DDS 纹理索引（来自 {@link #addDdsSource} 的返回值）
     * @param left           裁切区域左边界
     * @param up             裁切区域上边界
     * @param right          裁切区域右边界
     * @param down           裁切区域下边界
     * @param x              图像起始绘制点 X 坐标
     * @param y              图像起始绘制点 Y 坐标
     * @param frameWidth     帧域宽（基准用）
     * @param frameHeight    帧域高（基准用）
     */
    @Override
    public void addDdsImage(int ddsSourceIndex, int left, int up, int right, int down,
                            int x, int y, int frameWidth, int frameHeight) {
        DdsSource ddsSource = ddsTable != null ? ddsTable.get(ddsSourceIndex) : null;
        if (ddsSource == null) {
            throw new NpkException("DDS source not found for index " + ddsSourceIndex
                    + ". Call addDdsSource() first.");
        }
        // 帧的颜色系统直接沿用所引用 DDS 的像素格式（FXT1/FXT3/FXT5）
        int frameType = ddsSource.getPixelFormat();
        if (!Frame.isFxtType(frameType)) {
            throw new NpkException("DDS source at index " + ddsSourceIndex
                    + " has a non-FXT pixel format (0x" + Integer.toHexString(frameType)
                    + "); a DDS-reference frame requires FXT1/FXT3/FXT5 (0x12/0x13/0x14).");
        }
        if (right <= left || down <= up) {
            throw new NpkException("Invalid DDS crop region: ["
                    + left + "," + up + "," + right + "," + down + "]");
        }
        if (left < 0 || up < 0 || right > ddsSource.getWidth() || down > ddsSource.getHeight()) {
            throw new NpkException("DDS crop region [" + left + "," + up + "," + right + "," + down
                    + "] exceeds DDS source bounds " + ddsSource.getWidth() + "x" + ddsSource.getHeight());
        }
        if (frameWidth <= 0 || frameHeight <= 0) {
            throw new NpkException("Frame dimensions must be positive: "
                    + frameWidth + "x" + frameHeight);
        }
        Frame frame = new Frame(frameType);
        frame.setCompressed(Frame.COMPRESSED_DDS); // 规范：DDS 引用型索引项压缩状态为 7（ZLIB）
        frame.setWidth(right - left);
        frame.setHeight(down - up);
        frame.setLength(0);                // 保留字段（原图片大小位置），须为 0
        frame.setX(x);
        frame.setY(y);
        frame.setFrameWidth(frameWidth);
        frame.setFrameHeight(frameHeight);
        frame.setDdsIndex(ddsSourceIndex);
        frame.setLeftCut(left);
        frame.setUpCut(up);
        frame.setRightCut(right);
        frame.setDownCut(down);
        frames.add(frame);
        invalidateRawData();
    }
}
