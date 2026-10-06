package xyz.hooy.npkapi;

import xyz.hooy.npkapi.codec.DdsCodec;
import xyz.hooy.npkapi.codec.PixelCodec;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * IMG V5 格式实现 — 支持共享纹理图集（sprite/DDS）的图像。
 * <p>
 * V5 在 V2 的基础上增加了纹理表（{@link DdsTable}），并通过实现
 * {@link DdsCapable} 对外提供纹理能力。DDS 引用帧（压缩状态为
 * {@link Frame#COMPRESSED_DDS} 的帧）不直接存储像素数据，而是引用纹理表中的
 * 共享纹理，通过裁剪区域获取子图像，并可能带旋转标志（图集中旋转 90° 打包）。
 * </p>
 * <p>
 * 纹理像素格式分两类（详见 {@link DdsSource}）：DXT 压缩格式
 * （0x12/0x13/0x14）存储完整 DDS 文件；裸像素格式（0x0E/0x0F/0x10）
 * 直接存储未压缩像素字节。两类在真实游戏文件中均广泛存在。
 * </p>
 *
 * <h3>V5 头部扩展结构</h3>
 * <pre>
 * [V2 Header]
 * [Texture Table]
 *   - ddsCount (int32)
 *   - imgLength (int32, 整个 IMG 文件大小；写出时由 finishWrite 回填实际值)
 *   - colorNum (int32, V5 中无功能用途；读取时保存、写回时还原以保证往返保真)
 *   - palette[colorNum] (4 bytes each: R, G, B, A; 同上保真读写)
 *   - dds[ddsCount] (各 28 字节):
 *     - title (int32, 默认标头，一般为 1)
 *     - pixelFormat (int32)
 *     - index (int32)
 *     - compressedSize (int32, ZLIB 压缩后大小 = 数据区中该纹理块的实际字节数)
 *     - rawSize (int32, 压缩前大小；写出时按 ZLIB 压缩结果回填)
 *     - width (int32)
 *     - height (int32)
 * </pre>
 *
 * <h3>V5 帧表扩展与数据区顺序</h3>
 * <p>
 * 帧按「压缩状态」字段分派布局：值为 7（{@link Frame#COMPRESSED_DDS}）的是
 * DDS 引用帧（64 字节），在标准帧描述后附加裁剪信息；值为 5/6 的是普通点阵帧
 * （36 字节，颜色系统语义同 V2）；颜色系统 0x11 为指向帧（8 字节）。
 * <b>注意不能按颜色系统区分 DDS 引用帧</b>——其颜色系统取自所引用纹理的
 * 像素格式，真实文件中大量存在 0x10（ARGB8888）等与普通点阵帧相同的值：
 * </p>
 * <pre>
 * [标准帧描述 8 个 int（其中「大小」字段为保留，恒 0）]
 * [reserved (int32, 保留，一般为 0)]
 * [ddsIndex, leftCut, upCut, rightCut, downCut (各 int32)]
 * [rotate (int32, 旋转标志：1 = 图集中逆时针旋转 90° 存放，解码时还原)]
 * </pre>
 * <p>
 * 数据区按「纹理数据（ZLIB 压缩，按纹理表顺序）→ 普通点阵帧像素数据（按帧表顺序）」
 * 排列；DDS 引用帧与指向帧不对应任何数据块。
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

    /**
     * 无状态 DDS 引用帧编解码器单例
     */
    private static final PixelCodec DDS_CODEC = new DdsCodec();

    protected DdsTable ddsTable;

    /**
     * 写入阶段缓存的纹理压缩数据（index → ZLIB 压缩后字节）。
     * 由 {@link #prepareWrite()} 填充、{@link #finishWrite} 清空，
     * 保证头部写入的 compressedSize 字段与实际写出的数据长度一致。
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
        // 按帧类型注册 DDS 编解码器（DDS 引用帧的解码路由见 decodeFrame 覆写，
        // 此处注册保证 FXT 类型值亦可经通用 getCodec 路径解析）
        registerCodec(Frame.TYPE_FXT1, DDS_CODEC);
        registerCodec(Frame.TYPE_FXT3, DDS_CODEC);
        registerCodec(Frame.TYPE_FXT5, DDS_CODEC);
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
     * 登记一张纹理到纹理表，返回其分配的索引。
     * <p>
     * 传入的 {@link DdsSource} 须携带完整纹理数据：DXT 格式（0x12/0x13/0x14）
     * 为完整 DDS 文件字节（本库不实现 DXT 压缩）；裸像素格式（0x0E/0x0F/0x10）
     * 为 width × height × 每像素字节数的未压缩像素字节。
     * 压缩前/后大小会在写出阶段按 ZLIB 压缩结果自动回填。
     * </p>
     *
     * @param ddsSource 携带完整纹理数据与像素格式、宽高的纹理源
     * @return 该纹理在表中分配的索引，供 {@link #addDdsImage} 引用
     * @throws NpkException 纹理源为 null、缺少数据、数据与声明的格式/宽高不符，
     *                      或索引与既有纹理冲突
     */
    @Override
    public int addDdsSource(DdsSource ddsSource) {
        if (ddsSource == null) {
            throw new NpkException("DdsSource cannot be null.");
        }
        ddsSource.validateData(); // 数据非空、像素格式已知、尺寸/魔数与声明一致
        if (ddsTable == null) {
            ddsTable = new DdsTable();
        }
        ddsTable.put(ddsSource); // index < 0 时自动分配下一个可用索引；重复索引抛出 NpkException
        return ddsSource.getIndex();
    }

    /**
     * 追加一个不旋转的纹理裁切帧。
     *
     * @see #addDdsImage(int, Rectangle, Rectangle, boolean)
     */
    @Override
    public void addDdsImage(int ddsSourceIndex, Rectangle textureCrop, Rectangle frameCanvas) {
        addDdsImage(ddsSourceIndex, textureCrop, frameCanvas, false);
    }

    /**
     * 追加一个引用纹理裁切区域的 DDS 引用帧。
     * <p>
     * 帧的颜色系统无需显式传入——直接取自所引用纹理的像素格式
     * （0x0E/0x0F/0x10 裸像素或 0x12/0x13/0x14 DXT）。
     * 帧的贴图宽高由裁切区域得出；{@code rotated} 为 true 时表示该区域在图集中
     * 以逆时针 90° 旋转存放（打包优化），帧宽高为旋转还原后的尺寸（宽高互换），
     * 解码时自动还原。压缩状态按格式规范固定为 {@link Frame#COMPRESSED_DDS}，
     * 保留的 length 字段置 0。
     * </p>
     *
     * @param ddsSourceIndex 引用的纹理索引（来自 {@link #addDdsSource} 的返回值）
     * @param textureCrop    纹理裁切区域：x=左边界、y=上边界、width/height=区域尺寸；
     *                       须为正值且不得超出纹理源宽高
     * @param frameCanvas    帧画布：x/y=图像起始绘制点坐标，width/height=帧域尺寸（须为正值）
     * @param rotated        该区域是否在图集中旋转 90° 存放（解码时逆时针还原）
     * @throws NpkException 纹理索引不存在、裁切区域或画布参数非法
     */
    @Override
    public void addDdsImage(int ddsSourceIndex, Rectangle textureCrop, Rectangle frameCanvas,
                            boolean rotated) {
        if (textureCrop == null || frameCanvas == null) {
            throw new NpkException("textureCrop and frameCanvas cannot be null.");
        }
        DdsSource ddsSource = ddsTable != null ? ddsTable.get(ddsSourceIndex) : null;
        if (ddsSource == null) {
            throw new NpkException("DDS source not found for index " + ddsSourceIndex
                    + ". Call addDdsSource() first.");
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

        Frame frame = new Frame(ddsSource.getPixelFormat());
        frame.setCompressed(Frame.COMPRESSED_DDS); // 规范：DDS 引用帧压缩状态为 7（ZLIB sprite）
        // 帧贴图尺寸为裁切区域旋转还原后的尺寸
        frame.setWidth(rotated ? textureCrop.height : textureCrop.width);
        frame.setHeight(rotated ? textureCrop.width : textureCrop.height);
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
        frame.setRotate(rotated ? 1 : 0);
        frames.add(frame);
    }

    /**
     * DDS 引用帧（含 0x10 等与普通点阵帧同值的颜色系统）统一路由到
     * {@link DdsCodec}；其余帧按类型走通用编解码器注册表。
     */
    @Override
    protected BufferedImage decodeFrame(Frame frame) {
        if (frame.isDdsReference()) {
            return DDS_CODEC.decode(frame, createCodecContext());
        }
        return super.decodeFrame(frame);
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
            // 真实文件字段序：压缩后大小（数据区实际字节数）在前，压缩前大小在后
            ddsSource.setCompressedSize(stream.readInt());
            ddsSource.setRawSize(stream.readInt());
            ddsSource.setWidth(stream.readInt());
            ddsSource.setHeight(stream.readInt());
            if (ddsSource.getCompressedSize() < 0 || ddsSource.getRawSize() < 0) {
                throw new NpkException("Corrupt IMG v5: negative texture data size at index "
                        + ddsSource.getIndex() + " (compressedSize=" + ddsSource.getCompressedSize()
                        + ", rawSize=" + ddsSource.getRawSize() + ").");
            }
            table.put(ddsSource);
        }
        this.ddsTable = table;
    }

    /**
     * 写出前置准备：预先压缩所有纹理数据并回填 compressedSize/rawSize 字段，
     * 使头部表项声明的长度与数据区实际写出的字节一致。
     * <p>
     * 副作用说明：本方法会更新各 {@link DdsSource} 的大小元数据，
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
            ddsSource.setRawSize(data.length);
            ddsSource.setCompressedSize(compressed.length);
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
            // 大小字段已在 prepareWrite 阶段按 ZLIB 压缩结果回填，与缓存数据一致
            for (DdsSource ddsSource : ddsTable.values()) {
                stream.writeInt(ddsSource.getTitle());
                stream.writeInt(ddsSource.getPixelFormat());
                stream.writeInt(ddsSource.getIndex());
                stream.writeInt(ddsSource.getCompressedSize());
                stream.writeInt(ddsSource.getRawSize());
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
     * 读取数据区：先按纹理表顺序读取 ZLIB 压缩的纹理数据（解压上限为头部声明的
     * rawSize，防御压缩炸弹），再按帧表顺序读取普通点阵帧的像素数据。
     * <p>
     * DDS 引用帧与指向帧不对应任何数据块；DDS 引用帧的纹理由
     * {@link DdsCodec} 从 ddsTable 按 ddsIndex 获取。
     * </p>
     */
    @Override
    protected void readDataSection(ImageInputStream stream) throws IOException {
        if (ddsTable != null) {
            for (DdsSource ddsSource : ddsTable.values()) {
                byte[] compressed = new byte[ddsSource.getCompressedSize()];
                stream.readFully(compressed);
                ddsSource.setData(Bytes.decompress(compressed, ddsSource.getRawSize()));
            }
        }
        for (Frame frame : frames) {
            if (!frame.isReference() && !frame.isDdsReference()) {
                readFrameRawData(stream, frame);
            }
        }
    }

    /**
     * 写入数据区：先写 ZLIB 压缩的纹理数据（取自 prepareWrite 的缓存，
     * 与头部 compressedSize 字段一致），再按帧表顺序写普通点阵帧的像素数据。
     */
    @Override
    protected void writeDataSection(ImageOutputStream stream) throws IOException {
        if (ddsTable != null) {
            for (DdsSource ddsSource : ddsTable.values()) {
                stream.write(ddsCompressedCache.get(ddsSource.getIndex()));
            }
        }
        for (Frame frame : frames) {
            if (!frame.isReference() && !frame.isDdsReference()) {
                writeFrameRawData(stream, frame);
            }
        }
    }

    /**
     * 读取单帧描述。DDS 引用帧的判定依据是压缩状态字段（7），
     * 须在读取标准字段（含压缩状态）之后才能确定，故先走通用读取再补读扩展段。
     */
    @Override
    protected void readFrameDescriptor(ImageInputStream stream, Frame frame) throws IOException {
        super.readFrameDescriptor(stream, frame);
        if (!frame.isReference() && frame.isDdsReference()) {
            frame.setReserved(stream.readInt());
            frame.setDdsIndex(stream.readInt());
            frame.setLeftCut(stream.readInt());
            frame.setUpCut(stream.readInt());
            frame.setRightCut(stream.readInt());
            frame.setDownCut(stream.readInt());
            frame.setRotate(stream.readInt());
        }
    }

    @Override
    protected void writeFrameDescriptor(ImageOutputStream stream, Frame frame) throws IOException {
        // 帧 type 须先于描述体写入，与 readFrames「先 setType(readInt) 再 readFrameDescriptor」对称；
        // 若遗漏则每个帧少写 4 字节，重读时字节错位、纹理数据解压失败。
        stream.writeInt(frame.getType());
        if (frame.isReference()) {
            stream.writeInt(frame.getReference());
            return;
        }
        writeStandardFrameFields(stream, frame);
        if (frame.isDdsReference()) {
            stream.writeInt(frame.getReserved());
            stream.writeInt(frame.getDdsIndex());
            stream.writeInt(frame.getLeftCut());
            stream.writeInt(frame.getUpCut());
            stream.writeInt(frame.getRightCut());
            stream.writeInt(frame.getDownCut());
            stream.writeInt(frame.getRotate());
        }
    }

    @Override
    protected PixelCodec.CodecContext createCodecContext() {
        return new PixelCodec.CodecContext(ddsTable);
    }

    @Override
    protected int frameTableEntrySize(Frame frame) {
        if (frame.isDdsReference()) {
            return 64; // type(4) + 8 个 int 字段(32) + reserved(4) + 裁剪信息(20) + rotate(4)
        }
        return super.frameTableEntrySize(frame);
    }
}
