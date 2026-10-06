package xyz.hooy.npkapi;

/**
 * 调色板能力接口（{@link Version4Img} 与 {@link Version6Img} 实现）。
 * <p>
 * V4 为单一调色板（索引恒为 0），V6 支持多调色板配色方案
 * （见 {@link MultiPaletteCapable}）。其余版本不实现本接口，
 * 调用方应以 {@code instanceof} 探测能力。
 * </p>
 */
public interface PaletteCapable extends Img {

    /**
     * 获取调色板数量（V4 至多 1 个，V6 可多个）。
     *
     * @return 调色板数量
     */
    int getPaletteSourceCount();

    /**
     * 获取当前激活的调色板索引。
     *
     * @return 激活索引（V6 可通过 {@link #activatePaletteSource(int)} 切换）
     */
    int getActivePaletteSourceIndex();

    /**
     * 切换当前激活的调色板，切换后解码索引色帧使用新调色板。
     *
     * @param index 目标调色板索引（V4 仅允许 0）
     * @throws IndexOutOfBoundsException 索引越界
     */
    void activatePaletteSource(int index);

    /**
     * 按索引获取调色板。
     *
     * @param index 调色板索引（0 ~ getPaletteSourceCount()-1）
     * @return 对应调色板
     * @throws IndexOutOfBoundsException 索引越界
     */
    PaletteSource getPaletteSource(int index);
}
