package xyz.hooy.npkit;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * IMG 调色板。
 * <p>
 * 存储最多 256 种颜色（8 位索引），用于 IMG V4/V6 格式的索引色图像。
 * 颜色以 RGBA 顺序存储。
 * </p>
 */
public class PaletteSource {

    private static final int MAX_COLOR_SIZE = 256;

    private final List<Color> colors;

    public PaletteSource() {
        this.colors = new ArrayList<>(MAX_COLOR_SIZE);
    }

    public PaletteSource(List<Color> colors) {
        this.colors = new ArrayList<>(colors);
    }

    @Override
    public String toString() {
        return "Palette{size=" + colors.size() + "}";
    }

    /**
     * 获取指定索引处的颜色
     *
     * @param index 颜色索引（0~255）
     * @return 对应颜色
     */
    public Color get(int index) {
        return colors.get(index);
    }

    /**
     * 添加颜色到调色板末尾
     *
     * @param color 要添加的颜色
     * @throws NpkException 如果调色板已满（超过 256 色）
     */
    public void add(Color color) {
        if (colors.size() >= MAX_COLOR_SIZE) {
            throw new NpkException("Palette overflow: cannot exceed " + MAX_COLOR_SIZE + " colors.");
        }
        colors.add(color);
    }

    /**
     * 获取指定颜色在调色板中的索引
     *
     * @param color 要查找的颜色
     * @return 索引值（0~255），未找到返回 -1
     */
    public int indexOf(Color color) {
        return colors.indexOf(color);
    }

    /**
     * 获取调色板颜色数量
     */
    public int size() {
        return colors.size();
    }

    /**
     * 将调色板转为 RGBA 字节数组，用于二进制写入。
     * 每个颜色占 4 字节，按 R, G, B, A 顺序排列。
     */
    public byte[] toBytes() {
        byte[] bytes = new byte[colors.size() * 4];
        for (int i = 0; i < colors.size(); i++) {
            Color color = colors.get(i);
            bytes[i * 4] = (byte) color.getRed();
            bytes[i * 4 + 1] = (byte) color.getGreen();
            bytes[i * 4 + 2] = (byte) color.getBlue();
            bytes[i * 4 + 3] = (byte) color.getAlpha();
        }
        return bytes;
    }

    /**
     * 从 RGBA 字节数组解析调色板。
     * 每 4 字节解析为一个颜色（R, G, B, A 顺序）。
     *
     * @param bytes    RGBA 字节数组
     * @param colorNum 颜色数量
     * @return 解析后的调色板
     */
    public static PaletteSource fromBytes(byte[] bytes, int colorNum) {
        PaletteSource palette = new PaletteSource();
        for (int i = 0; i < colorNum; i++) {
            int offset = i * 4;
            int r = bytes[offset] & 0xFF;
            int g = bytes[offset + 1] & 0xFF;
            int b = bytes[offset + 2] & 0xFF;
            int a = bytes[offset + 3] & 0xFF;
            palette.add(new Color(r, g, b, a));
        }
        return palette;
    }
}
