package xyz.hooy.npkapi;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * IMG V1 格式实现 — 早期交错布局图像。
 * <p>
 * V1 是 DNF 早期客户端使用的 IMG 版本，现存资源极少（据格式资料，国服客户端
 * 仅 sprite_interface_iga_iga08.npk 内有两个 V1 IMG）。与 V2+ 的差异：
 * </p>
 * <ul>
 *   <li>魔数为旧版 18 字节 {@code "Neople Image File\0"}（V2+ 为 16 字节
 *       {@code "Neople Img File\0"}）；</li>
 *   <li>头部无「索引表大小」字段，代之以 int16 未知字段（实践中为 0）；</li>
 *   <li>帧采用<b>交错布局</b>：每帧描述符之后立即跟随该帧像素数据，
 *       而非 V2+ 的「集中帧表 + 集中数据区」；</li>
 *   <li>未压缩帧（compressed=5）描述符中的 length 字段不可靠，
 *       实际数据长度须按 宽 × 高 × 每像素字节数 计算。</li>
 * </ul>
 * <p>
 * 像素格式语义同 V2（0x0E=ARGB1555、0x0F=ARGB4444、0x10=ARGB8888、
 * 0x11=指向帧），无调色板与纹理表。
 * </p>
 * <p>
 * 布局依据多个独立实现（PyDnfEx 读写、DNFToolKit 读取、Koishi 头部解析）
 * 交叉验证；因 V1 真实样本极罕见，写出路径按同一约定的规范值生成
 * （未知字段/keep 读出保留、写回还原，新建文件置 0）。
 * </p>
 *
 * <h3>V1 文件结构</h3>
 * <pre>
 * [Header]
 *   - IMG_MAGIC_OLD (18 bytes): "Neople Image File\0"
 *   - unknown (int16, 实践中为 0)
 *   - keep (int32, 实践中为 0)
 *   - version (int32, = 1)      ← 与 V2+ 相同，位于偏移 24
 *   - frameCount (int32)
 * [Frames]（交错布局）
 *   - frame[i] 描述符:
 *       指向型: type(0x11) + linkTo               (8 字节，无数据)
 *       图像型: type + compressed + w + h + length
 *               + x + y + frameWidth + frameHeight (36 字节)
 *   - frame[i] 像素数据（紧跟描述符；compressed=5 时长度按 w*h*bpp 修正）
 * </pre>
 *
 * @see AbstractImg
 */
public class Version1Img extends AbstractImg {

    /**
     * V1 旧版魔数："Neople Image File\0"（18 字节）。
     * 包私有以避免公开可变数组被外部篡改；对外需要时使用 {@link #getImgMagicOld()}。
     */
    static final byte[] IMG_MAGIC_OLD = {
            'N', 'e', 'o', 'p', 'l', 'e', ' ', 'I', 'm', 'a', 'g', 'e',
            ' ', 'F', 'i', 'l', 'e', '\0'
    };

    /**
     * 旧版魔数长度（字节）
     */
    public static final int MAGIC_OLD_LENGTH = 18;

    /**
     * 头部 int16 未知字段（实践中恒为 0；读出保留、写回还原以保证往返保真）
     */
    private short headerUnknown;

    /**
     * 头部 keep 字段（对应 V2+ 的 padding 位置，实践中恒为 0；同上保真读写）
     */
    private int headerKeep;

    /**
     * 获取 V1 旧版魔数的副本。
     */
    public static byte[] getImgMagicOld() {
        return IMG_MAGIC_OLD.clone();
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    protected void readHeader(ImageInputStream stream) throws IOException {
        byte[] magic = new byte[IMG_MAGIC_OLD.length];
        stream.readFully(magic);
        if (!Arrays.equals(IMG_MAGIC_OLD, magic)) {
            throw new NpkException("Invalid IMG v1 file: magic bytes mismatch. "
                    + "Expected 'Neople Image File\\0' (18 bytes).");
        }
        this.headerUnknown = stream.readShort();
        this.headerKeep = stream.readInt();
        int version = stream.readInt();
        if (getVersion() != version) {
            throw new NpkException(getClass().getSimpleName() + " cannot process IMG v" + version
                    + " (expected v" + getVersion() + ")");
        }
        this.frameCount = stream.readInt();
        if (frameCount < 0) {
            throw new NpkException("Corrupt IMG v1: negative frame count " + frameCount + ".");
        }
    }

    @Override
    protected void writeHeader(ImageOutputStream stream) throws IOException {
        stream.write(IMG_MAGIC_OLD);
        stream.writeShort(headerUnknown);
        stream.writeInt(headerKeep);
        stream.writeInt(getVersion());
        stream.writeInt(frames.size());
    }

    /**
     * 读取所有帧：V1 为交错布局，每帧描述符之后立即读取其像素数据。
     */
    @Override
    protected void readFrames(ImageInputStream stream) throws IOException {
        List<Frame> frameList = new ArrayList<>(frameCount);
        for (int i = 0; i < frameCount; i++) {
            Frame frame = new Frame();
            frame.setType(stream.readInt());
            readFrameDescriptor(stream, frame);
            if (!frame.isReference()) {
                readFrameRawData(stream, frame);
            }
            frameList.add(frame);
        }
        this.frames = frameList;
    }

    /**
     * 写入所有帧：与读取对称的交错布局（描述符 + 数据逐帧写出）。
     */
    @Override
    protected void writeFrames(ImageOutputStream stream) throws IOException {
        for (Frame frame : frames) {
            writeFrameDescriptor(stream, frame);
            if (!frame.isReference()) {
                writeFrameRawData(stream, frame);
            }
        }
    }

    /**
     * 读取帧像素数据。
     * <p>
     * V1 未压缩帧（compressed=5）描述符中的 length 字段不可靠，
     * 实际数据长度按 宽 × 高 × 每像素字节数 计算（多个独立实现一致采用此修正）；
     * ZLIB 压缩帧（compressed=6）的 length 为压缩后字节数，可信任。
     * </p>
     */
    @Override
    protected void readFrameRawData(ImageInputStream stream, Frame frame) throws IOException {
        if (frame.getCompressed() == Frame.UNCOMPRESSED) {
            int expected = frame.expectedDataSize(rawBytesPerPixel(frame.getType()));
            byte[] data = new byte[expected];
            stream.readFully(data);
            frame.setRawData(data);
        } else {
            super.readFrameRawData(stream, frame);
        }
    }

    /**
     * V1 像素格式的每像素字节数（语义同 V2：0x10=ARGB8888 为 4，
     * 0x0E=ARGB1555 / 0x0F=ARGB4444 为 2）。
     *
     * @throws NpkException 未知像素格式（无法推导未压缩帧的数据长度）
     */
    private static int rawBytesPerPixel(int frameType) {
        if (frameType == Frame.TYPE_ARGB8888) {
            return 4;
        }
        if (frameType == Frame.TYPE_ARGB1555 || frameType == Frame.TYPE_ARGB4444) {
            return 2;
        }
        throw new NpkException("Unsupported IMG v1 pixel format 0x"
                + Integer.toHexString(frameType)
                + " for an uncompressed frame; cannot derive its data size.");
    }
}
