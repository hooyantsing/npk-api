package xyz.hooy.npkapi;

/**
 * IMG 文件中的帧数据模型。
 * <p>
 * 帧是 IMG 文件中的基本图像单元，一个 IMG 文件包含多个帧。
 * 帧可以是以下类型之一：
 * <ul>
 *   <li>图像帧（ARGB1555/ARGB4444/ARGB8888）— 直接存储像素数据</li>
 *   <li>索引帧（INDEXED）— 在 V4/V6 上下文中，type 值 0x0E 表示索引色</li>
 *   <li>引用帧（REFERENCE）— 指向另一个帧的索引</li>
 *   <li>DDS 帧（FXT1/FXT3/FXT5）— 引用 DdsTable 中的纹理数据</li>
 * </ul>
 *
 * <h3>关于 TYPE_INDEXED 与 TYPE_ARGB1555</h3>
 * <p>
 * 在 DNF NPK 格式中，帧类型值 {@code 0x0E} 在不同 IMG 版本中有不同含义：
 * <ul>
 *   <li>IMG V2：{@code 0x0E} 表示 ARGB1555 像素格式</li>
 *   <li>IMG V4/V6：{@code 0x0E} 表示索引色（调色板）格式</li>
 * </ul>
 * 两者共享相同的类型值，由 IMG 版本决定语义解释。
 * </p>
 *
 * <h3>length 字段语义</h3>
 * <p>
 * 帧描述中的 length 字段对图像帧而言是像素数据字节数，对 FXT 帧而言是保留字段（须为 0）。
 * 本类中该字段不由外部单独维护：{@link #getLength()} 在帧挂载了像素数据时
 * 直接返回数据长度（单一事实来源），仅在无数据时（FXT 帧、描述解析阶段）
 * 返回 {@link #setLength(int)} 记录的描述值。
 * </p>
 */
public class Frame {

    /**
     * 在 V4/V6 上下文中表示索引色帧。
     * 注意：与 {@link #TYPE_ARGB1555} 共享相同的原始值 0x0E，
     * 具体语义由 IMG 版本决定。
     */
    public static final int TYPE_INDEXED = 0x0E;

    /**
     * ARGB 1555 格式（1 bit alpha + 5 bit RGB），在 V2 上下文中 type=0x0E
     */
    public static final int TYPE_ARGB1555 = 0x0E;

    /**
     * ARGB 4444 格式（4 bit ARGB）
     */
    public static final int TYPE_ARGB4444 = 0x0F;

    /**
     * ARGB 8888 格式（8 bit ARGB）
     */
    public static final int TYPE_ARGB8888 = 0x10;

    /**
     * 引用帧，指向同 IMG 内另一帧的索引
     */
    public static final int TYPE_REFERENCE = 0x11;

    /**
     * DDS FXT1 压缩纹理
     */
    public static final int TYPE_FXT1 = 0x12;

    /**
     * DDS FXT3 压缩纹理
     */
    public static final int TYPE_FXT3 = 0x13;

    /**
     * DDS FXT5 压缩纹理
     */
    public static final int TYPE_FXT5 = 0x14;

    /**
     * 帧数据压缩标志。
     * <ul>
     *   <li>{@link #UNCOMPRESSED} (0x05)：未压缩</li>
     *   <li>{@link #COMPRESSED} (0x06)：ZLIB 压缩（普通图像帧）</li>
     *   <li>{@link #COMPRESSED_DDS} (0x07)：ZLIB 压缩（DDS 引用型索引项）</li>
     * </ul>
     */
    public static final int UNCOMPRESSED = 0x05;

    /**
     * ZLIB 压缩（普通图像帧）
     */
    public static final int COMPRESSED = 0x06;

    /**
     * ZLIB 压缩（DDS 引用型索引项，见 {@link DdsCapable#addDdsImage}）
     */
    public static final int COMPRESSED_DDS = 0x07;

    private int type;

    /** 引用帧：指向的帧索引 */
    private int reference;

    /**
     * 图像帧：压缩标志
     */
    private int compressed = COMPRESSED;

    /**
     * 图像帧：像素宽度
     */
    private int width;

    /**
     * 图像帧：像素高度
     */
    private int height;

    /**
     * 帧描述中的 length 字段值（见类文档「length 字段语义」）
     */
    private int length;

    /**
     * 图像帧：X 偏移
     */
    private int x;

    /**
     * 图像帧：Y 偏移
     */
    private int y;

    /**
     * 图像帧：画布宽度
     */
    private int frameWidth;

    /**
     * 图像帧：画布高度
     */
    private int frameHeight;

    /**
     * 图像帧：原始像素数据（可能是压缩的）
     */
    private byte[] rawData;

    /**
     * DDS 帧：DDS 表中的索引
     */
    private int ddsIndex;

    /**
     * DDS 帧：裁剪区域 - 左
     */
    private int leftCut;

    /**
     * DDS 帧：裁剪区域 - 上
     */
    private int upCut;

    /**
     * DDS 帧：裁剪区域 - 右
     */
    private int rightCut;

    /**
     * DDS 帧：裁剪区域 - 下
     */
    private int downCut;

    public Frame() {
    }

    public Frame(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        if (isReference()) {
            return "Frame{REFERENCE -> " + reference + "}";
        } else if (isFxt()) {
            return "Frame{FXT, ddsIndex=" + ddsIndex
                    + ", cut=[" + leftCut + "," + upCut + "," + rightCut + "," + downCut + "]}";
        } else {
            return "Frame{type=0x" + Integer.toHexString(type)
                    + ", " + width + "x" + height
                    + ", length=" + getLength() + "}";
        }
    }

    public boolean isArgb() {
        return isArgbType(type);
    }

    public boolean isReference() {
        return type == TYPE_REFERENCE;
    }

    public boolean isFxt() {
        return isFxtType(type);
    }

    public boolean isCompressed() {
        return compressed == COMPRESSED;
    }

    public static boolean isArgbType(int type) {
        return type == TYPE_ARGB1555 || type == TYPE_ARGB4444 || type == TYPE_ARGB8888;
    }

    public static boolean isFxtType(int type) {
        return type == TYPE_FXT1 || type == TYPE_FXT3 || type == TYPE_FXT5;
    }

    /**
     * 计算本帧像素数据的期望字节数（宽 × 高 × 每像素字节数）。
     *
     * @param bytesPerPixel 每像素字节数（索引色 1、16 位色 2、ARGB8888 4）
     * @return 期望字节数
     * @throws NpkException 宽高为负或乘积超出 int 范围（损坏文件的防御）
     */
    public int expectedDataSize(int bytesPerPixel) {
        long size = (long) width * height * bytesPerPixel;
        if (width < 0 || height < 0 || size > Integer.MAX_VALUE) {
            throw new NpkException("Frame data size overflow: " + width + "x" + height
                    + " x " + bytesPerPixel + " bytes/pixel.");
        }
        return (int) size;
    }

    /**
     * 获取解压后的原始像素数据，并校验其长度恰为期望值。
     * <p>
     * 返回值始终是独立副本：调用方（如像素编解码器将其包装进
     * {@code DataBuffer}）修改返回数组不会影响帧内部状态。
     * 压缩数据解压时以 {@code expectedSize} 为上限，防御压缩炸弹。
     * </p>
     *
     * @param expectedSize 期望的数据字节数（通常来自 {@link #expectedDataSize(int)}）
     * @return 解压后的独立字节数组，长度恰为 expectedSize
     * @throws NpkException 帧无原始像素数据（引用帧、FXT 帧或未挂载数据的帧），
     *                      或数据长度与期望不符、解压失败/超限
     */
    public byte[] getDecompressedData(int expectedSize) {
        if (rawData == null) {
            throw new NpkException("Frame has no raw data (type=0x"
                    + Integer.toHexString(type) + ").");
        }
        if (expectedSize < 0) {
            throw new IllegalArgumentException("expectedSize must be >= 0: " + expectedSize);
        }
        byte[] data = isCompressed() ? Bytes.decompress(rawData, expectedSize) : rawData.clone();
        if (data.length != expectedSize) {
            throw new NpkException("Frame data length " + data.length
                    + " does not match expected size " + expectedSize + " (" + this + ").");
        }
        return data;
    }

    /**
     * 压缩并设置原始数据。
     * 根据当前压缩标志决定是否对数据进行 zlib 压缩。
     *
     * @param uncompressedData 未压缩的原始像素数据
     */
    public void compressAndSetData(byte[] uncompressedData) {
        if (isCompressed()) {
            this.rawData = Bytes.compress(uncompressedData);
        } else {
            this.rawData = uncompressedData;
        }
        this.length = this.rawData.length;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getReference() {
        return reference;
    }

    public void setReference(int reference) {
        this.reference = reference;
    }

    public int getCompressed() {
        return compressed;
    }

    public void setCompressed(int compressed) {
        this.compressed = compressed;
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
     * 帧描述中的 length 值：挂载了像素数据时为数据长度（单一事实来源），
     * 否则为描述解析/保留字段值（FXT 帧恒为 0）。
     */
    public int getLength() {
        return rawData != null ? rawData.length : length;
    }

    /**
     * 设置帧描述中的 length 字段值。
     * 仅对未挂载像素数据的帧有效（FXT 保留字段、描述解析阶段）；
     * 挂载数据后 {@link #getLength()} 以数据长度为准，本值被忽略。
     */
    public void setLength(int length) {
        this.length = length;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    public void setFrameWidth(int frameWidth) {
        this.frameWidth = frameWidth;
    }

    public int getFrameHeight() {
        return frameHeight;
    }

    public void setFrameHeight(int frameHeight) {
        this.frameHeight = frameHeight;
    }

    /**
     * 帧的原始像素字节（可能是压缩的），仅供序列化与编解码使用。
     * 注意：返回内部引用，调用方不应修改其内容。
     */
    public byte[] getRawData() {
        return rawData;
    }

    public void setRawData(byte[] rawData) {
        this.rawData = rawData;
        this.length = rawData != null ? rawData.length : 0;
    }

    public int getDdsIndex() {
        return ddsIndex;
    }

    public void setDdsIndex(int ddsIndex) {
        this.ddsIndex = ddsIndex;
    }

    public int getLeftCut() {
        return leftCut;
    }

    public void setLeftCut(int leftCut) {
        this.leftCut = leftCut;
    }

    public int getUpCut() {
        return upCut;
    }

    public void setUpCut(int upCut) {
        this.upCut = upCut;
    }

    public int getRightCut() {
        return rightCut;
    }

    public void setRightCut(int rightCut) {
        this.rightCut = rightCut;
    }

    public int getDownCut() {
        return downCut;
    }

    public void setDownCut(int downCut) {
        this.downCut = downCut;
    }
}
