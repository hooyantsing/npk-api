package xyz.hooy.npkit;

import java.awt.image.BufferedImage;

/**
 * IMG 图像资源接口。
 * <p>
 * IMG 是 NPK 文件中的图像资源格式，支持多个版本（V2/V4/V5/V6），
 * 每个版本有不同的像素编码方式和附加数据结构。
 * </p>
 * <p>
 * 一个 IMG 资源包含若干帧（Frame），每帧可解码为一张 {@link BufferedImage}。
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
     * 获取 IMG 文件版本号
     *
     * @return 版本号（2, 4, 5, 6 等）
     */
    int version();

    /**
     * 获取帧（图像）总数
     */
    int getImageCount();

    /**
     * 获取指定索引的帧解码后的图像。
     * 如果是引用帧，会自动解引用到目标帧。
     *
     * @param index 帧索引
     * @return 解码后的 BufferedImage
     * @throws IndexOutOfBoundsException 索引越界
     * @throws NpkException              解码失败
     */
    BufferedImage getImage(int index);

    /**
     * 添加一帧图像，以指定的像素编码格式存储
     *
     * @param image     待添加的图像
     * @param frameType 帧类型（如 {@link Frame#TYPE_ARGB8888}）
     */
    void addImage(BufferedImage image, int frameType);

    /**
     * 添加一个引用帧，指向已存在的帧
     *
     * @param referenceIndex 被引用帧的索引
     */
    void addReference(int referenceIndex);

    /**
     * 获取帧列表（只读视图）
     */
    java.util.List<Frame> getFrames();

    // ===== DDS 相关（仅 V5 支持） =====

    /**
     * 获取 DDS 纹理源数量。
     * 非 V5 版本返回 0。
     */
    int getDdsSourceCount();

    /**
     * 获取指定索引的 DDS 纹理源图像
     *
     * @param index DDS 表索引
     * @return DDS 解码后的图像，不支持时返回 null
     */
    BufferedImage getDdsSource(int index);

    /**
     * 获取 DDS 纹理表
     *
     * @return DDS 纹理表，不支持时返回 null
     */
    DdsTable getDdsTable();

    // ===== 调色板相关（V4/V6 支持） =====

    /**
     * 获取调色板数量。
     * V4 返回 1，V6 返回多个，其他版本返回 0。
     */
    int getPaletteCount();

    /**
     * 获取指定索引的调色板
     *
     * @param index 调色板索引
     * @return 调色板对象，不支持时返回 null
     */
    Palette getPalette(int index);

    /**
     * 激活指定索引的调色板作为当前解码使用的调色板。
     * 仅 V6（多调色板）有意义。
     *
     * @param index 调色板索引
     */
    void activatePalette(int index);

    /**
     * 获取当前激活的调色板
     *
     * @return 当前调色板，不支持时返回 null
     */
    Palette getActivePalette();

    // ===== NpkResource 默认实现 =====

    @Override
    default NpkResource.Type getType() {
        return NpkResource.Type.IMG;
    }
}
