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
 * IMG V2 格式实现 — 所有版本 IMG 的基类。
 * <p>
 * IMG V2 支持 ARGB1555、ARGB4444、ARGB8888 三种像素格式和引用帧。
 * 采用模板方法模式，子类（V4/V5/V6）可覆写各阶段方法以扩展功能。
 * </p>
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
 */
public class Version2Img implements Img {

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

    public Version2Img() {
        codecs.put(Frame.TYPE_ARGB1555, new Argb1555Codec());
        codecs.put(Frame.TYPE_ARGB4444, new Argb4444Codec());
        codecs.put(Frame.TYPE_ARGB8888, new Argb8888Codec());
    }

    // ===== Img 接口实现 =====

    @Override
    public int version() {
        return 2;
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

    @Override
    public void addImage(BufferedImage image, int frameType) {
        PixelCodec codec = getCodec(frameType);
        Frame frame = codec.encode(image, createCodecContext());
        frames.add(frame);
        invalidateRawData();
    }

    @Override
    public void addReference(int referenceIndex) {
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
     * 使缓存的原始字节失效。
     * <p>
     * IMG 从文件加载时会缓存原始字节（见 {@link #setRawData(byte[])}），
     * 以便未修改时可原样快速重存。一旦帧等内部结构被修改，必须调用本方法
     * 清空缓存，使后续保存时重新序列化，避免修改被静默丢弃。
     * </p>
     */
    protected void invalidateRawData() {
        this.rawData = null;
    }

    @Override
    public List<Frame> getFrames() {
        return Collections.unmodifiableList(frames);
    }

    @Override
    public int getDdsSourceCount() {
        return 0;
    }

    @Override
    public BufferedImage getDdsSource(int index) {
        return null;
    }

    @Override
    public DdsTable getDdsTable() {
        return null;
    }

    @Override
    public int getPaletteCount() {
        return 0;
    }

    @Override
    public Palette getPalette(int index) {
        return null;
    }

    @Override
    public void activatePalette(int index) {
        // V2 不支持调色板
    }

    @Override
    public Palette getActivePalette() {
        return null;
    }

    // ===== Processor 接口实现（读写） =====

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

    // ===== 模板方法：Header 读写 =====

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
        stream.readInt();          // tableLength (unused, can be recalculated)
        stream.skipBytes(4);       // padding
        int version = stream.readInt();
        if (version() != version) {
            throw new NpkException(getClass().getSimpleName() + " cannot process IMG v" + version
                    + " (expected v" + version() + ")");
        }
        this.frameSize = stream.readInt();
    }

    /**
     * 写入 IMG 文件头。
     */
    protected void writeHeader(ImageOutputStream stream) throws IOException {
        stream.write(IMG_MAGIC);
        int tableLength = calculateTableLength();
        stream.writeInt(tableLength);
        stream.writeInt(0);           // padding
        stream.writeInt(version());
        stream.writeInt(frames.size());
    }

    // ===== 模板方法：Frame 读写 =====

    /**
     * 读取所有帧（先读帧表，再读帧数据）。
     */
    protected void readFrames(ImageInputStream stream) throws IOException {
        List<Frame> frameList = new ArrayList<>(frameSize);
        for (int i = 0; i < frameSize; i++) {
            Frame frame = new Frame();
            frame.setType(stream.readInt());
            readFrame(stream, frame);
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
     */
    protected void readFrame(ImageInputStream stream, Frame frame) throws IOException {
        if (frame.isReference()) {
            frame.setReference(stream.readInt());
        } else {
            frame.setCompressed(stream.readInt());
            frame.setWidth(stream.readInt());
            frame.setHeight(stream.readInt());
            frame.setLength(stream.readInt());
            frame.setX(stream.readInt());
            frame.setY(stream.readInt());
            frame.setFrameWidth(stream.readInt());
            frame.setFrameHeight(stream.readInt());
        }
    }

    /**
     * 读取帧的原始像素数据。
     */
    protected void readFrameRawData(ImageInputStream stream, Frame frame) throws IOException {
        byte[] data = new byte[frame.getLength()];
        stream.readFully(data);
        frame.setRawData(data);
    }

    /**
     * 写入所有帧（先写帧表，再写帧数据）。
     */
    protected void writeFrames(ImageOutputStream stream) throws IOException {
        for (Frame frame : frames) {
            writeFrame(stream, frame);
        }
        for (Frame frame : frames) {
            if (!frame.isReference()) {
                writeFrameRawData(stream, frame);
            }
        }
    }

    /**
     * 写入单帧描述信息。
     */
    protected void writeFrame(ImageOutputStream stream, Frame frame) throws IOException {
        stream.writeInt(frame.getType());
        if (frame.isReference()) {
            stream.writeInt(frame.getReference());
        } else {
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

    /**
     * 写入帧原始像素数据。
     */
    protected void writeFrameRawData(ImageOutputStream stream, Frame frame) throws IOException {
        stream.write(frame.getRawData());
    }

    // ===== 编解码辅助 =====

    /**
     * 将帧解码为 BufferedImage。
     * 子类可覆写以支持额外的帧类型（如 DDS、INDEXED）。
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

    // ===== 内部工具 =====

    /**
     * 计算帧表总长度（字节）
     */
    protected int calculateTableLength() {
        int length = 0;
        for (Frame frame : frames) {
            length += 4; // type field
            if (frame.isReference()) {
                length += 4; // reference index
            } else {
                length += 32; // 8 int fields
            }
        }
        return length;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{name='" + name + "', version=" + version()
                + ", frames=" + frames.size() + "}";
    }
}
