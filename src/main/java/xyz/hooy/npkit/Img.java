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
 * <p>
 * DDS 纹理能力仅 V5（{@link Version5Img}）真正支持：其余版本调用相关方法时
 * 返回空值或抛出 {@link NpkException}（见 AbstractImg 的默认实现）。
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

    int getVersion();

    int getImageCount();

    BufferedImage getImage(int index);

    void addImage(BufferedImage image, int frameType);

    void addReferenceImage(int referenceIndex);

    boolean supportDdsSource();

    int getDdsSourceCount();

    DdsSource getDdsSource(int index);

    int addDdsSource(DdsSource ddsSource);

    void addDdsImage(int ddsSourceIndex, int left, int up, int right, int down, int x, int y, int frameWidth, int frameHeight);

    boolean supportPaletteSource();

    int getPaletteSourceCount();

    void activatePaletteSource(int index);

    int getActivePaletteSourceIndex();

    void addPalettesImage(BufferedImage[] palettesImage);
}
