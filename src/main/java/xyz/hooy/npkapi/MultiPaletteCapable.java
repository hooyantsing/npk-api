package xyz.hooy.npkapi;

import java.awt.image.BufferedImage;

/**
 * 多调色板配色能力接口（仅 {@link Version6Img} 实现）。
 * <p>
 * 同一帧形状可对应多套配色方案（如装备染色、多队伍颜色区分），
 * 各调色板须锁步增长、共享同一「索引 → 区域」映射。
 * </p>
 */
public interface MultiPaletteCapable extends PaletteCapable {

    /**
     * 以多调色板配色方案追加一帧。
     * <p>
     * 各图像须尺寸一致且共享同一「形状分区」（仅颜色可不同），
     * 数组长度须与既有调色板数量一致；详细约束与锁步机制见
     * {@link Version6Img#addMultiPaletteImage(BufferedImage[])}。
     * </p>
     *
     * @param paletteImages 各调色板对应的配色图像（paletteImages[0] 为索引分区参考图）
     * @throws NpkException 图像组不满足多调色板约束或调色板溢出
     */
    void addMultiPaletteImage(BufferedImage[] paletteImages);
}
