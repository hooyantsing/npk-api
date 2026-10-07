package xyz.hooy.npkapi;

import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * IMG 图像资源接口（核心能力）。
 * <p>
 * IMG 是 NPK 文件中的图像资源格式，支持多个版本（V1/V2/V3/V4/V5/V6，
 * 其中 V1 为早期交错布局、V3 为无公开样本的过渡版本 best-effort 支持），
 * 每个版本有不同的像素编码方式和附加数据结构。
 * </p>
 * <p>
 * 一个 IMG 资源包含若干帧（{@link Frame}），每帧可解码为一张 {@link BufferedImage}。
 * </p>
 * <p>
 * 版本特有的扩展能力按接口隔离原则拆分为能力接口，
 * 通过 {@code instanceof} 探测后再强转使用：
 * </p>
 * <ul>
 *   <li>{@link DdsCapable} — DDS 纹理源与 FXT 帧（仅 V5 实现）</li>
 *   <li>{@link PaletteCapable} — 调色板（V4/V6 实现）</li>
 *   <li>{@link MultiPaletteCapable} — 多调色板配色方案（仅 V6 实现）</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * Img img = ImgFactory.create(2);
 * img.read(imageInputStream);
 * BufferedImage sprite = img.getImage(0);
 *
 * if (img instanceof PaletteCapable) {
 *     PaletteSource palette = ((PaletteCapable) img).getPaletteSource(0);
 * }
 * }</pre>
 *
 * @see ImgFactory
 * @see Version1Img
 * @see Version2Img
 * @see Version3Img
 * @see Version4Img
 * @see Version5Img
 * @see Version6Img
 */
public interface Img extends NpkResource {

    /**
     * 从输入流中读取并解析 IMG 数据（小端序）。
     *
     * @param stream 小端序图像输入流
     * @throws IOException  读取异常
     * @throws NpkException 魔数/版本不匹配或文件损坏
     */
    void read(ImageInputStream stream) throws IOException;

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
     * 获取指定帧的数据模型（描述字段与像素字节）。
     * <p>
     * 用于读取帧的绘制偏移（x/y）、画布尺寸（frameWidth/frameHeight）等
     * 解码为 {@link BufferedImage} 时不携带的元数据。
     * 注意：返回的帧是内部可变对象，直接修改其描述字段会影响后续写出。
     * </p>
     *
     * @param index 帧索引（0 ~ getImageCount()-1）
     * @return 对应帧
     * @throws IndexOutOfBoundsException 帧索引越界
     */
    Frame getFrame(int index);

    /**
     * 解码指定帧为图像，引用帧自动解引用。
     * <p>
     * 返回的图像与帧内部数据完全独立，修改其像素不会影响本资源，
     * 再次保存时也不会写出被修改的内容。
     * </p>
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
     * V6 在多调色板状态下禁止使用本方法（须改用
     * {@link MultiPaletteCapable#addMultiPaletteImage(BufferedImage[])}）。
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
}
