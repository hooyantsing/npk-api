package xyz.hooy.npkapi;

import xyz.hooy.npkapi.codec.DdsCodec;
import xyz.hooy.npkapi.codec.PixelCodec;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * IMG V5 格式实现 — 支持 DDS 纹理的图像。
 * <p>
 * V5 在 V2 的基础上增加了 DDS 纹理表（{@link DdsTable}），
 * 并通过实现 {@link DdsCapable} 对外提供 DDS 能力。
 * FXT1/FXT3/FXT5 类型的帧不直接存储像素数据，
 * 而是引用 DDS 表中的共享纹理，并通过裁剪区域获取子图像。
 * </p>
 *
 * <h3>V5 头部扩展结构</h3>
 * <pre>
 * [V2 Header]
 * [DDS Table]
 *   - ddsCount (int32)
 *   - imgLength (int32, 整个 IMG 文件大小；写出时由 finishWrite 回填实际值)
 *   - colorNum (int32, V5 中无功能用途；读取时保存、写回时还原以保证往返保真)
 *   - palette[colorNum] (4 bytes each: R, G, B, A; 同上保真读写)
 *   - dds[ddsCount]:
 *     - title (int32)
 *     - pixelFormat (int32)
 *     - index (int32)
 *     - fullLength (int32, 压缩前数据长度；写出时按 ZLIB 压缩结果回填)
 *     - length (int32, 压缩后数据长度；写出时按 ZLIB 压缩结果回填)
 *     - width (int32)
 *     - height (int32)
 * </pre>
 *
 * <h3>V5 帧表扩展与数据区顺序</h3>
 * <p>
 * FXT 类型帧在标准帧描述后附加 DDS 裁剪信息（前后各有一个 4 字节 padding）：
 * </p>
 * <pre>
 * [标准帧描述 8 个 int]
 * [padding (int32)]
 * [ddsIndex, leftCut, upCut, rightCut, downCut (各 int32)]
 * [padding (int32)]
 * </pre>
 * <p>
 * 数据区按「DDS 纹理数据（ZLIB 压缩）→ 非 FXT/非引用帧像素数据」的顺序排列；
 * FXT 帧本身不持有像素数据，其纹理由 {@link DdsCodec} 按 ddsIndex 从 DDS 表获取。
 * </p>
 */
public class Version5Img extends AbstractImg implements DdsCapable {

    /**
     * V5 头部中「整个 IMG 文件大小」字段（imgLength）相对 IMG 起始位置的偏移：
     * magic(16) + tableLength(4) + padding(4) + version(4) + frameCount(4) + ddsCount(4)。
     * 头部先于帧数据写出，总大小此刻未知，故在 {@link #finishWrite} 中按
     * 写出起始位置相对回填（不假设流从位置 0 开始）。
     */
    private static final int IMG_LENGTH_FIELD_OFFSET = AbstractImg.MAGIC_LENGTH + 4 + 4 + 4 + 4 + 4;

    protected DdsTable ddsTable;

    /**
     * 写入阶段缓存的 DDS 压缩数据（index → ZLIB 压缩后字节）。
     * 由 {@link #prepareWrite()} 填充、{@link #finishWrite} 清空，
     * 保证头部写入的 length 字段与实际写出的数据长度一致。
     */
    private final Map<Integer, byte[]> ddsCompressedCache = new LinkedHashMap<>();

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
    public int getDdsSourceCount() {
        return ddsTable != null ? ddsTable.size() : 0;
    }

    @Override
    public DdsSource getDdsSource(int index) {
        DdsSource ddsSource = ddsTable != null ? ddsTable.get(index) : null;
        if (ddsSource == null) {
            throw new IndexOutOfBoundsException("DDS source index " + index
                    + " out of bounds [0, " + getDdsSourceCount() + ")");
        }
        return ddsSource;
    }

    /**
     * 登记一张 DDS 纹理到 DDS 表，返回其分配的索引。
     * <p>
     * 传入的 {@link DdsSource} 须携带完整的 DDS 文件字节
     * （本库不实现 BufferedImage → DDS 的 DXT 压缩）。
     * 压缩前/后大小（fullLength/length）会在写出阶段按 ZLIB 压缩结果自动回填。
     * </p>
     *
     * @param ddsSource 携带完整 DDS 文件字节与像素格式、宽高的纹理源
     * @return 该 DDS 在表中分配的索引，供 {@link #addDdsImage} 引用
     * @throws NpkException 纹理源为 null、缺少数据或索引与既有纹理冲突
     */
    @Override
    public int addDdsSource(DdsSource ddsSource) {
        if (ddsSource == null || ddsSource.getData() == null || ddsSource.getData().length == 0) {
            throw new NpkException("DDS data cannot be null or empty.");
        }
        if (ddsTable == null) {
            ddsTable = new DdsTable();
        }
        ddsTable.put(ddsSource); // index < 0 时自动分配下一个可用索引；重复索引抛出 NpkException
        return ddsSource.getIndex();
    }

    /**
     * 追加一个引用 DDS 纹理裁切区域的 FXT 帧（DDS 引用型索引项）。
     * <p>
     * 帧的颜色系统（type）无需显式传入——一帧即从所引用的 DDS 大图裁切而来，
     * 其压缩方式必然等于该 DDS 的像素格式，故直接取自 {@code DdsSource.pixelFormat}
     * （取值 0x12/0x13/0x14，与 {@link Frame#TYPE_FXT1} 等同一编码空间）。
     * 帧的贴图宽高由裁切区域自动得出，压缩状态按格式规范固定为
     * {@link Frame#COMPRESSED_DDS}，保留的 length 字段置 0。
     * </p>
     *
     * @param ddsSourceIndex 引用的 DDS 纹理索引（来自 {@link #addDdsSource} 的返回值）
     * @param textureCrop    纹理裁切区域：x=左边界、y=上边界、width/height=区域尺寸；
     *                       须为正值且不得超出纹理源宽高
     * @param frameCanvas    帧画布：x/y=图像起始绘制点坐标，width/height=帧域尺寸（基准用，须为正值）
     * @throws NpkException DDS 索引不存在、像素格式非 FXT、裁切区域或画布参数非法
     */
    @Override
    public void addDdsImage(int ddsSourceIndex, Rectangle textureCrop, Rectangle frameCanvas) {
        if (textureCrop == null || frameCanvas == null) {
            throw new NpkException("textureCrop and frameCanvas cannot be null.");
        }
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
        if (textureCrop.width <= 0 || textureCrop.height <= 0) {
            throw new NpkException("Invalid DDS crop region (width/height must be positive): "
                    + textureCrop);
        }
        long cropRight = (long) textureCrop.x + textureCrop.width;
        long cropDown = (long) textureCrop.y + textureCrop.height;
        if (textureCrop.x < 0 || textureCrop.y < 0
                || cropRight > ddsSource.getWidth() || cropDown > ddsSource.getHeight()) {
            throw new NpkException("DDS crop region " + textureCrop
                    + " exceeds DDS source bounds "
                    + ddsSource.getWidth() + "x" + ddsSource.getHeight() + ".");
        }
        if (frameCanvas.width <= 0 || frameCanvas.height <= 0) {
            throw new NpkException("Frame canvas dimensions must be positive: "
                    + frameCanvas.width + "x" + frameCanvas.height);
        }

        Frame frame = new Frame(frameType);
        frame.setCompressed(Frame.COMPRESSED_DDS); // 规范：DDS 引用型索引项压缩状态为 7（ZLIB）
        frame.setWidth(textureCrop.width);
        frame.setHeight(textureCrop.height);
        frame.setLength(0);                // 保留字段（原图片大小位置），须为 0
        frame.setX(frameCanvas.x);
        frame.setY(frameCanvas.y);
        frame.setFrameWidth(frameCanvas.width);
        frame.setFrameHeight(frameCanvas.height);
        frame.setDdsIndex(ddsSourceIndex);
        frame.setLeftCut(textureCrop.x);
        frame.setUpCut(textureCrop.y);
        frame.setRightCut((int) cropRight);
        frame.setDownCut((int) cropDown);
        frames.add(frame);
    }

    @Override
    protected void readHeader(ImageInputStream stream) throws IOException {
        super.readHeader(stream);

        int ddsCount = stream.readInt();
        if (ddsCount < 0) {
            throw new NpkException("Corrupt IMG v5: negative DDS count " + ddsCount + ".");
        }
        stream.readInt();                  // imgLength（整个 IMG 文件大小，写出时由 finishWrite 回填实际值）
        int colorNum = stream.readInt();   // V5 头部调色板颜色数目（绝大部分为 0）
        if (colorNum < 0 || colorNum > PaletteSource.MAX_COLOR_SIZE) {
            throw new NpkException("Corrupt IMG v5: invalid header palette color count " + colorNum + ".");
        }
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
            if (ddsSource.getFullLength() < 0 || ddsSource.getLength() < 0) {
                throw new NpkException("Corrupt IMG v5: negative DDS data length at index "
                        + ddsSource.getIndex() + " (fullLength=" + ddsSource.getFullLength()
                        + ", length=" + ddsSource.getLength() + ").");
            }
            table.put(ddsSource);
        }
        this.ddsTable = table;
    }

    /**
     * 写出前置准备：预先压缩所有 DDS 数据并回填 fullLength/length 字段，
     * 使头部表项声明的长度与数据区实际写出的字节一致。
     * <p>
     * 副作用说明：本方法会更新各 {@link DdsSource} 的 fullLength/length 元数据，
     * 使其反映最近一次写出的实际值。
     * </p>
     */
    @Override
    protected void prepareWrite() {
        ddsCompressedCache.clear();
        if (ddsTable == null) {
            return;
        }
        for (DdsSource ddsSource : ddsTable.values()) {
            byte[] data = ddsSource.getData();
            if (data == null) {
                throw new NpkException("DDS source at index " + ddsSource.getIndex()
                        + " has no data to write.");
            }
            byte[] compressed = Bytes.compress(data);
            ddsSource.setFullLength(data.length);
            ddsSource.setLength(compressed.length);
            ddsCompressedCache.put(ddsSource.getIndex(), compressed);
        }
    }

    @Override
    protected void writeHeader(ImageOutputStream stream) throws IOException {
        super.writeHeader(stream);

        int ddsCount = ddsTable != null ? ddsTable.size() : 0;
        stream.writeInt(ddsCount);
        stream.writeInt(0); // imgLength 占位（整个 IMG 文件大小），由 finishWrite 回填实际值
        // V5 头部调色板：在 V5 中无用途，但极少数文件保留有颜色项，需往返保真
        if (headerPalette != null && headerPalette.size() > 0) {
            stream.writeInt(headerPalette.size());
            stream.write(headerPalette.toBytes());
        } else {
            stream.writeInt(0);
        }

        if (ddsTable != null) {
            // fullLength/length 已在 prepareWrite 阶段按 ZLIB 压缩结果回填，与缓存数据一致
            for (DdsSource ddsSource : ddsTable.values()) {
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

    /**
     * 写出收尾：回填头部的 imgLength 字段（相对写出起始位置，
     * 不依赖流从位置 0 开始的假设），并清理压缩缓存。
     */
    @Override
    protected void finishWrite(ImageOutputStream stream, long startPosition) throws IOException {
        long endPosition = stream.getStreamPosition();
        stream.seek(startPosition + IMG_LENGTH_FIELD_OFFSET);
        stream.writeInt((int) (endPosition - startPosition));
        stream.seek(endPosition);
        ddsCompressedCache.clear();
    }

    /**
     * 读取数据区：先读 ZLIB 压缩的 DDS 纹理数据（解压上限为头部声明的 fullLength，
     * 防御压缩炸弹），再读非 FXT/非引用帧的像素数据。
     * <p>
     * FXT 帧不持有自身像素数据，其 DDS 纹理由 DdsCodec 从 ddsTable 按 ddsIndex 获取。
     * </p>
     */
    @Override
    protected void readDataSection(ImageInputStream stream) throws IOException {
        if (ddsTable != null) {
            for (DdsSource ddsSource : ddsTable.values()) {
                byte[] compressed = new byte[ddsSource.getLength()];
                stream.readFully(compressed);
                ddsSource.setData(Bytes.decompress(compressed, ddsSource.getFullLength()));
            }
        }
        for (Frame frame : frames) {
            if (!frame.isReference() && !frame.isFxt()) {
                readFrameRawData(stream, frame);
            }
        }
    }

    /**
     * 写入数据区：先写 ZLIB 压缩的 DDS 纹理数据（取自 prepareWrite 的缓存，
     * 与头部 length 字段一致），再写非 FXT/非引用帧的像素数据。
     */
    @Override
    protected void writeDataSection(ImageOutputStream stream) throws IOException {
        if (ddsTable != null) {
            for (DdsSource ddsSource : ddsTable.values()) {
                stream.write(ddsCompressedCache.get(ddsSource.getIndex()));
            }
        }
        for (Frame frame : frames) {
            if (!frame.isReference() && !frame.isFxt()) {
                writeFrameRawData(stream, frame);
            }
        }
    }

    @Override
    protected void readFrameDescriptor(ImageInputStream stream, Frame frame) throws IOException {
        if (frame.isFxt()) {
            // FXT 帧：标准图像帧描述 + DDS 裁剪信息
            readStandardFrameFields(stream, frame);
            Bytes.skipFully(stream, 4); // padding
            frame.setDdsIndex(stream.readInt());
            frame.setLeftCut(stream.readInt());
            frame.setUpCut(stream.readInt());
            frame.setRightCut(stream.readInt());
            frame.setDownCut(stream.readInt());
            Bytes.skipFully(stream, 4); // padding
        } else {
            super.readFrameDescriptor(stream, frame);
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
}
