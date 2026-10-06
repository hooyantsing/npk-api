package xyz.hooy.npkapi;

import java.awt.image.BufferedImage;

/**
 * IMG 图像资源接口。
 * <p>
 * IMG 是 NPK 文件中的图像资源格式，支持多个版本（V2/V4/V5/V6），
 * 每个版本有不同的像素编码方式和附加数据结构。
 * </p>
 * <p>
 * 一个 IMG 资源包含若干帧（{@link Frame}），每帧可解码为一张 {@link BufferedImage}。
 * </p>
 * <p>
 * DDS 纹理能力仅 V5（{@link Version5Img}）真正支持：其余版本调用相关方法时
 * 返回空值或抛出 {@link NpkException}（见 {@link AbstractImg} 的默认实现）。
 * </p>
 * <p>
 * 调色板能力仅 V4/V6（{@link Version4Img}）真正支持：V4 为单一调色板，
 * V6 支持多调色板配色方案；其余版本同理降级。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * Img img = ImgFactory.create(2);
 * img.read(imageInputStream);
 * BufferedImage sprite = img.getImage(0);
 * }</pre>
 *
 * @see ImgFactory
 * @see Version2Img
 * @see Version4Img
 * @see Version5Img
 * @see Version6Img
 */
public interface Img extends NpkResource, Processor {

    /**
     * 获取 IMG 格式版本号。
     *
     * @return 版本号（2/4/5/6）
     */
    int getVersion();

    /**
     * 获取帧数量（含引用帧与 FXT 帧）。
     *
     * @return 帧总数
     */
    int getImageCount();

    /**
     * 解码指定帧为图像，引用帧自动解引用。
     *
     * @param index 帧索引（0 ~ getImageCount()-1）
     * @return 解码后的图像
     * @throws IndexOutOfBoundsException 帧索引越界
     * @throws NpkException              检测到循环引用或帧数据解码失败
     */
    BufferedImage getImage(int index);

    /**
     * 追加一帧并按指定帧类型编码。
     * <p>
     * 注意：{@link Frame#TYPE_INDEXED} 与 {@link Frame#TYPE_ARGB1555} 共享类型值 0x0E，
     * 语义由 IMG 版本决定——V4/V6 编码为索引色并向当前调色板追加图像中的新颜色
     * （超过 256 色抛出 {@link NpkException}），V2 编码为 ARGB1555；
     * V6 在多调色板状态下禁止使用本方法（须改用 {@link #addPalettesImage(BufferedImage[])}）。
     * </p>
     *
     * @param image     源图像
     * @param frameType 帧类型（如 {@link Frame#TYPE_ARGB8888}）
     * @throws NpkException 帧类型无对应编解码器，或编码失败（如调色板溢出）
     */
    void addImage(BufferedImage image, int frameType);

    /**
     * 追加一个引用帧，指向既有帧（不复制像素数据，解码时自动解引用）。
     *
     * @param referenceIndex 被引用的帧索引
     * @throws IndexOutOfBoundsException 引用索引越界
     */
    void addReferenceImage(int referenceIndex);

    /**
     * 当前版本是否支持 DDS 纹理源（仅 V5 为 true）。
     *
     * @return 是否支持
     */
    boolean supportDdsSource();

    /**
     * 获取 DDS 纹理源数量（不支持 DDS 的版本恒为 0）。
     *
     * @return DDS 纹理源数量
     */
    int getDdsSourceCount();

    /**
     * 按索引获取 DDS 纹理源。
     *
     * @param index DDS 索引（0 ~ getDdsSourceCount()-1）
     * @return 对应纹理源；索引无效或当前版本不支持 DDS 时返回 null
     */
    DdsSource getDdsSource(int index);

    /**
     * 登记一张 DDS 纹理源并返回其分配索引（仅 V5 支持）。
     * <p>
     * 传入的 {@link DdsSource} 须携带完整的 DDS 文件字节；
     * 本库不提供 BufferedImage → DDS 的 DXT 压缩能力。
     * </p>
     *
     * @param ddsSource 携带完整 DDS 文件字节的纹理源
     * @return 分配的 DDS 索引，供 {@link #addDdsImage} 引用
     * @throws NpkException 当前版本不支持 DDS，或纹理源数据为 null/空
     */
    int addDdsSource(DdsSource ddsSource);

    /**
     * 追加一个引用 DDS 纹理裁切区域的 FXT 帧（仅 V5 支持）。
     * <p>
     * 帧类型取自所引用 DDS 的像素格式，贴图宽高由裁切区域得出；
     * 详细参数约束见
     * {@link Version5Img#addDdsImage(int, int, int, int, int, int, int, int, int)}。
     * </p>
     *
     * @param ddsSourceIndex DDS 索引（来自 {@link #addDdsSource} 的返回值）
     * @param left           裁切区域左边界（含）
     * @param up             裁切区域上边界（含）
     * @param right          裁切区域右边界（不含）
     * @param down           裁切区域下边界（不含）
     * @param x              图像起始绘制点 X 坐标
     * @param y              图像起始绘制点 Y 坐标
     * @param frameWidth     帧域宽
     * @param frameHeight    帧域高
     * @throws NpkException 当前版本不支持 DDS，或索引/像素格式/裁切区域/帧域参数非法
     */
    void addDdsImage(int ddsSourceIndex, int left, int up, int right, int down, int x, int y, int frameWidth, int frameHeight);

    /**
     * 当前版本是否支持调色板（V4/V6 为 true）。
     *
     * @return 是否支持
     */
    boolean supportPaletteSource();

    /**
     * 获取调色板数量（V4 至多 1 个，V6 可多个；不支持的版本恒为 0）。
     *
     * @return 调色板数量
     */
    int getPaletteSourceCount();

    /**
     * 获取当前激活的调色板索引。
     *
     * @return 激活索引（V6 可通过 {@link #activatePaletteSource(int)} 切换）
     * @throws NpkException 当前版本不支持调色板
     */
    int getActivePaletteSourceIndex();

    /**
     * 切换当前激活的调色板，切换后解码索引色帧使用新调色板。
     *
     * @param index 目标调色板索引（V4 仅允许 0）
     * @throws NpkException              当前版本不支持调色板
     * @throws IndexOutOfBoundsException 索引越界
     */
    void activatePaletteSource(int index);

    /**
     * 按索引获取调色板。
     * <p>
     * 注意：返回的是可变对象，直接修改不会使缓存的原始字节失效，
     * 再次保存时修改可能被缓存的原始字节覆盖。
     * </p>
     *
     * @param index 调色板索引（0 ~ getPaletteSourceCount()-1）
     * @return 对应调色板
     * @throws NpkException              当前版本不支持调色板
     * @throws IndexOutOfBoundsException 索引越界
     */
    PaletteSource getPaletteSource(int index);

    /**
     * 以多调色板配色方案追加一帧（仅 V6 支持）。
     * <p>
     * 各图像须尺寸一致且共享同一「形状分区」（仅颜色可不同），
     * 数组长度须与既有调色板数量一致；详细约束与锁步机制见
     * {@link Version6Img#addPalettesImage(BufferedImage[])}。
     * </p>
     *
     * @param palettesImage 各调色板对应的配色图像
     * @throws NpkException 当前版本不支持，或图像组不满足多调色板约束
     */
    void addPalettesImage(BufferedImage[] palettesImage);
}
