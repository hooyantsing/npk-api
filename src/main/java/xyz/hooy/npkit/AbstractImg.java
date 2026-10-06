package xyz.hooy.npkit;

import xyz.hooy.npkit.codec.Argb1555Codec;
import xyz.hooy.npkit.codec.Argb4444Codec;
import xyz.hooy.npkit.codec.Argb8888Codec;
import xyz.hooy.npkit.codec.PixelCodec;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

/**
 * IMG 各版本实现的模板基类。
 * <p>
 * 采用模板方法模式固化「文件头 → 帧表 → 帧数据」的读写流程，
 * 子类（V2/V4/V5/V6）通过覆写各阶段钩子方法扩展版本特有的结构：
 * </p>
 * <ul>
 *   <li>{@link #readHeader}/{@link #writeHeader} — 头部扩展（调色板、DDS 表等）</li>
 *   <li>{@link #readFrameDescriptor}/{@link #writeFrameDescriptor} — 版本特有的帧描述布局</li>
 *   <li>{@link #createCodecContext} — 为编解码器提供调色板、DDS 表等附加信息</li>
 * </ul>
 *
 * <h3>文件结构</h3>
 * <pre>
 * [Header]
 *   - IMG_MAGIC (16 bytes)
 *   - tableLength (int32)
 *   - padding (int32)
 *   - version (int32)
 *   - frameCount (int32)
 * [Frame Table]
 *   - frame[0..n] 描述信息
 * [Frame Data]
 *   - frame[0..n] 原始像素数据
 * </pre>
 *
 * @see Version2Img
 * @see Version4Img
 * @see Version5Img
 * @see Version6Img
 */
public abstract class AbstractImg implements Img {

    /**
     * IMG 文件魔数标识
     */
    public static final byte[] IMG_MAGIC = {
            'N', 'e', 'o', 'p', 'l', 'e', ' ', 'I', 'm', 'g', ' ', 'F', 'i', 'l', 'e', '\0'
    };

    protected String name;
    protected byte[] rawData;
    protected List<Frame> frames = new ArrayList<>();
    protected int frameSize;

    /**
     * 像素编解码器注册表：帧类型值 → 编解码器
     */
    protected final Map<Integer, PixelCodec> codecs = new LinkedHashMap<>();

    /**
     * 无状态编解码器单例：所有 Img 实例共享，避免每次构造重复分配。
     * 编解码器不持有实例状态（状态全部来自方法参数 frame/context/image），故可安全共享。
     */
    private static final PixelCodec ARGB1555_CODEC = new Argb1555Codec();
    private static final PixelCodec ARGB4444_CODEC = new Argb4444Codec();
    private static final PixelCodec ARGB8888_CODEC = new Argb8888Codec();

    public AbstractImg() {
        codecs.put(Frame.TYPE_ARGB1555, ARGB1555_CODEC);
        codecs.put(Frame.TYPE_ARGB4444, ARGB4444_CODEC);
        codecs.put(Frame.TYPE_ARGB8888, ARGB8888_CODEC);
    }

    @Override
    public NpkResource.Type getType() {
        return NpkResource.Type.IMG;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public byte[] getRawData() {
        return rawData;
    }

    @Override
    public void setRawData(byte[] rawData) {
        this.rawData = rawData;
    }

    @Override
    public int getImageCount() {
        return frames.size();
    }

    @Override
    public BufferedImage getImage(int index) {
        return getImage(index, new HashSet<Integer>());
    }

    @Override
    public void addImage(BufferedImage image, int frameType) {
        PixelCodec codec = getCodec(frameType);
        Frame frame = codec.encode(image, createCodecContext());
        frames.add(frame);
        invalidateRawData();
    }

    @Override
    public void addReferenceImage(int referenceIndex) {
        if (referenceIndex < 0 || referenceIndex >= frames.size()) {
            throw new IndexOutOfBoundsException(
                    "Reference index " + referenceIndex + " out of bounds [0, " + frames.size() + ")");
        }
        Frame frame = new Frame(Frame.TYPE_REFERENCE);
        frame.setReference(referenceIndex);
        frames.add(frame);
        invalidateRawData();
    }

    /**
     * 本版本是否支持 DDS 纹理源（默认不支持，仅 V5 覆写为 true）
     */
    @Override
    public boolean supportDdsSource() {
        return false;
    }

    @Override
    public int getDdsSourceCount() {
        return 0;
    }

    @Override
    public DdsSource getDdsSource(int index) {
        return null;
    }

    @Override
    public int addDdsSource(DdsSource ddsSource) {
        throw new NpkException(getClass().getSimpleName()
                + " does not support DDS sources; only Version5Img does.");
    }

    @Override
    public void addDdsImage(int ddsSourceIndex, int left, int up, int right, int down,
                            int x, int y, int frameWidth, int frameHeight) {
        throw new NpkException(getClass().getSimpleName()
                + " does not support DDS frames; only Version5Img does.");
    }

    /**
     * 本版本是否支持调色板（默认不支持，仅 V4/V6 覆写为 true）
     */
    @Override
    public boolean supportPaletteSource() {
        return false;
    }

    @Override
    public int getPaletteSourceCount() {
        return 0;
    }

    @Override
    public int getActivePaletteSourceIndex() {
        throw new NpkException(getClass().getSimpleName()
                + " does not support palettes; only Version4Img/Version6Img do.");
    }

    @Override
    public void activatePaletteSource(int index) {
        throw new NpkException(getClass().getSimpleName()
                + " does not support palettes; only Version4Img/Version6Img do.");
    }

    @Override
    public PaletteSource getPaletteSource(int index) {
        throw new NpkException(getClass().getSimpleName()
                + " does not support palettes; only Version4Img/Version6Img do.");
    }

    @Override
    public void addPalettesImage(BufferedImage[] palettesImage) {
        throw new NpkException(getClass().getSimpleName()
                + " does not support multi-palette images; only Version6Img does.");
    }

    @Override
    public void read(ImageInputStream stream) throws IOException {
        readHeader(stream);
        readFrames(stream);
    }

    @Override
    public void write(ImageOutputStream stream) throws IOException {
        writeHeader(stream);
        writeFrames(stream);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{name='" + name + "', version=" + getVersion()
                + ", frames=" + frames.size() + "}";
    }

    /**
     * 带环检测的帧解码：记录已访问的帧索引，
     * 若某索引被重复访问则说明存在循环引用，抛出 {@link NpkException} 而非栈溢出。
     */
    private BufferedImage getImage(int index, Set<Integer> visited) {
        if (index < 0 || index >= frames.size()) {
            throw new IndexOutOfBoundsException(
                    "Frame index " + index + " out of bounds [0, " + frames.size() + ")");
        }
        if (!visited.add(index)) {
            throw new NpkException("Circular frame reference detected at index " + index);
        }
        Frame frame = frames.get(index);
        if (frame.isReference()) {
            return getImage(frame.getReference(), visited);
        }
        return decodeFrame(frame);
    }

    /**
     * 使缓存的原始字节失效。
     * <p>
     * IMG 从文件加载时会缓存原始字节（见 {@link #setRawData(byte[])}），
     * 以便未修改时可原样快速重存。一旦帧等内部结构被修改，必须调用本方法
     * 清空缓存，使后续保存时重新序列化，避免修改被静默丢弃。
     * </p>
     * <p>
     * 注意：addImage / addReferenceImage / addDdsSource / addDdsImage /
     * addPalettesImage 等方法会自动调用本方法；但通过 {@link #getPaletteSource(int)}
     * 或 {@link #getDdsSource(int)} 拿到的 PaletteSource / DdsSource 是可变对象，
     * 直接修改它们不会触发失效，再次保存时修改会被缓存的原始字节覆盖。
     * </p>
     */
    protected void invalidateRawData() {
        this.rawData = null;
    }

    /**
     * 读取 IMG 文件头。
     * 子类可覆写以读取额外的头部信息（如调色板、DDS 表等）。
     */
    protected void readHeader(ImageInputStream stream) throws IOException {
        byte[] magic = new byte[IMG_MAGIC.length];
        stream.readFully(magic);
        if (!Arrays.equals(IMG_MAGIC, magic)) {
            throw new NpkException("Invalid IMG file: magic bytes mismatch.");
        }
        stream.readInt();               // tableLength (unused, can be recalculated)
        Bytes.skipFully(stream, 4);     // padding（skipBytes 在截断文件上会静默少跳）
        int version = stream.readInt();
        if (getVersion() != version) {
            throw new NpkException(getClass().getSimpleName() + " cannot process IMG v" + version
                    + " (expected v" + getVersion() + ")");
        }
        this.frameSize = stream.readInt();
        if (frameSize < 0) {
            throw new NpkException("Corrupt IMG: negative frame count " + frameSize + ".");
        }
    }

    /**
     * 写入 IMG 文件头。
     */
    protected void writeHeader(ImageOutputStream stream) throws IOException {
        stream.write(IMG_MAGIC);
        int tableLength = calculateTableLength();
        stream.writeInt(tableLength);
        stream.writeInt(0);           // padding
        stream.writeInt(getVersion());
        stream.writeInt(frames.size());
    }

    /**
     * 读取所有帧（先读帧表，再读帧数据）。
     */
    protected void readFrames(ImageInputStream stream) throws IOException {
        List<Frame> frameList = new ArrayList<>(frameSize);
        for (int i = 0; i < frameSize; i++) {
            Frame frame = new Frame();
            frame.setType(stream.readInt());
            readFrameDescriptor(stream, frame);
            frameList.add(frame);
        }
        for (int i = 0; i < frameSize; i++) {
            Frame frame = frameList.get(i);
            if (!frame.isReference()) {
                readFrameRawData(stream, frame);
            }
        }
        this.frames = frameList;
    }

    /**
     * 读取单帧的描述信息（不含像素数据）。
     * 子类可覆写以支持扩展的帧描述布局（如 V5 的 FXT 裁剪字段）。
     */
    protected void readFrameDescriptor(ImageInputStream stream, Frame frame) throws IOException {
        if (frame.isReference()) {
            frame.setReference(stream.readInt());
        } else {
            readStandardFrameFields(stream, frame);
        }
    }

    /**
     * 读取标准图像帧的 8 个 int 描述字段
     * （compressed、width、height、length、x、y、frameWidth、frameHeight）。
     * 普通帧与 FXT 帧的描述均以此开头。
     */
    protected void readStandardFrameFields(ImageInputStream stream, Frame frame) throws IOException {
        frame.setCompressed(stream.readInt());
        frame.setWidth(stream.readInt());
        frame.setHeight(stream.readInt());
        frame.setLength(stream.readInt());
        frame.setX(stream.readInt());
        frame.setY(stream.readInt());
        frame.setFrameWidth(stream.readInt());
        frame.setFrameHeight(stream.readInt());
        if (frame.getWidth() < 0 || frame.getHeight() < 0) {
            throw new NpkException("Corrupt IMG: negative frame dimensions "
                    + frame.getWidth() + "x" + frame.getHeight() + ".");
        }
    }

    /**
     * 读取帧的原始像素数据。
     */
    protected void readFrameRawData(ImageInputStream stream, Frame frame) throws IOException {
        int length = frame.getLength();
        if (length < 0) {
            throw new NpkException("Corrupt IMG: negative frame data length " + length + ".");
        }
        byte[] data = new byte[length];
        stream.readFully(data);
        frame.setRawData(data);
    }

    /**
     * 写入所有帧（先写帧表，再写帧数据）。
     */
    protected void writeFrames(ImageOutputStream stream) throws IOException {
        for (Frame frame : frames) {
            writeFrameDescriptor(stream, frame);
        }
        for (Frame frame : frames) {
            if (!frame.isReference()) {
                writeFrameRawData(stream, frame);
            }
        }
    }

    /**
     * 写入单帧描述信息。
     * 子类可覆写以支持扩展的帧描述布局（如 V5 的 FXT 裁剪字段）。
     */
    protected void writeFrameDescriptor(ImageOutputStream stream, Frame frame) throws IOException {
        stream.writeInt(frame.getType());
        if (frame.isReference()) {
            stream.writeInt(frame.getReference());
        } else {
            writeStandardFrameFields(stream, frame);
        }
    }

    /**
     * 写入标准图像帧的 8 个 int 描述字段。
     * 普通帧与 FXT 帧的描述均以此开头。
     */
    protected void writeStandardFrameFields(ImageOutputStream stream, Frame frame) throws IOException {
        stream.writeInt(frame.getCompressed());
        stream.writeInt(frame.getWidth());
        stream.writeInt(frame.getHeight());
        stream.writeInt(frame.getLength());
        stream.writeInt(frame.getX());
        stream.writeInt(frame.getY());
        stream.writeInt(frame.getFrameWidth());
        stream.writeInt(frame.getFrameHeight());
    }

    /**
     * 写入帧原始像素数据。
     */
    protected void writeFrameRawData(ImageOutputStream stream, Frame frame) throws IOException {
        stream.write(frame.getRawData());
    }

    /**
     * 将帧解码为 BufferedImage。
     * 默认按帧类型从编解码器注册表取对应策略，由具体版本类的注册情况决定语义
     * （如 0x0E 在 V2 中为 ARGB1555、在 V4/V6 中为索引色）。
     */
    protected BufferedImage decodeFrame(Frame frame) {
        PixelCodec codec = getCodec(frame.getType());
        return codec.decode(frame, createCodecContext());
    }

    /**
     * 创建编解码上下文。
     * 子类可覆写以提供调色板、DDS 表等附加信息。
     */
    protected PixelCodec.CodecContext createCodecContext() {
        return new PixelCodec.CodecContext();
    }

    /**
     * 获取指定帧类型的编解码器
     */
    protected PixelCodec getCodec(int frameType) {
        PixelCodec codec = codecs.get(frameType);
        if (codec == null) {
            throw new NpkException("No codec registered for frame type: 0x"
                    + Integer.toHexString(frameType));
        }
        return codec;
    }

    /**
     * 注册编解码器（供子类调用）
     */
    protected void registerCodec(int frameType, PixelCodec codec) {
        codecs.put(frameType, codec);
    }

    /**
     * 计算帧表总长度（字节）
     */
    protected int calculateTableLength() {
        int length = 0;
        for (Frame frame : frames) {
            length += frameTableEntrySize(frame);
        }
        return length;
    }

    /**
     * 单个帧在帧表中占据的字节数（含 type 字段）。
     * 子类可覆写以支持扩展的帧描述布局。
     */
    protected int frameTableEntrySize(Frame frame) {
        if (frame.isReference()) {
            return 8;   // type(4) + reference(4)
        }
        return 36;      // type(4) + 8 个 int 字段(32)
    }
}
