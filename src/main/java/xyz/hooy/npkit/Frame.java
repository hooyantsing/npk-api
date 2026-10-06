package xyz.hooy.npkit;

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
 * </p>
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
     * ZLIB 压缩（DDS 引用型索引项，见 {@link Version5Img#addDdsImage}）
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
     * 图像帧：原始数据长度（字节）
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
                    + ", length=" + length + "}";
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
     * 获取解压后的原始像素数据。
     * 如果帧数据是压缩的，会自动解压；否则直接返回原始数据。
     *
     * @return 解压后的字节数组
     * @throws NpkException 帧无原始像素数据时抛出（如手工构造但未挂载数据的帧）
     */
    public byte[] getDecompressedData() {
        if (rawData == null) {
            throw new NpkException("Frame has no raw data (type=0x"
                    + Integer.toHexString(type) + ").");
        }
        if (isCompressed()) {
            return Bytes.decompress(rawData);
        }
        return rawData;
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

    public int getLength() {
        return length;
    }

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
