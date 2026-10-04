package xyz.hooy.npkit;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * IMG V6 格式实现 — 多调色板索引色图像。
 * <p>
 * V6 在 V4 的基础上支持多个调色板，可通过 {@link #activatePalette(int)}
 * 切换当前使用的调色板。常用于同一 IMG 中不同帧使用不同配色的场景
 * （如装备染色、多队伍颜色区分等）。
 * </p>
 *
 * <h3>V6 头部扩展结构</h3>
 * <pre>
 * [V2 Header]
 * [Palettes]  （V6 直接以颜色方案数开头，无 V4 的单一主调色板）
 *   - paletteCount (int32)
 *   - palettes[paletteCount]:
 *     - colorNum (int32)
 *     - colors[colorNum] (4 bytes each: R, G, B, A)
 * </pre>
 */
public class Version6Img extends Version4Img {

    private List<Palette> palettes = new ArrayList<>();
    private int activePaletteIndex = 0;

    public Version6Img() {
        super();
        // 初始化一个空调色板方案，使新建的 V6 可直接 addImage；
        // 从文件读取时 readPalette 会覆盖此列表
        this.palettes.add(new Palette());
        this.palette = this.palettes.get(0);
        this.activePaletteIndex = 0;
    }

    @Override
    public int version() {
        return 6;
    }

    @Override
    public int getPaletteCount() {
        return palettes.size();
    }

    @Override
    public Palette getPalette(int index) {
        return palettes.get(index);
    }

    @Override
    public void activatePalette(int index) {
        if (index < 0 || index >= palettes.size()) {
            throw new IndexOutOfBoundsException(
                    "Palette index " + index + " out of bounds [0, " + palettes.size() + ")");
        }
        this.activePaletteIndex = index;
        this.palette = palettes.get(index);
    }

    @Override
    public Palette getActivePalette() {
        return palette;
    }

    /**
     * 读取 V6 调色板数据。
     * <p>
     * V6 头部之后直接是「颜色方案数」，随后按顺序存储每个颜色方案
     * （每方案：颜色数目 + 4×颜色数目字节），不存在 V4 的单一主调色板。
     * </p>
     */
    @Override
    protected void readPalette(ImageInputStream stream) throws IOException {
        int paletteCount = stream.readInt();
        List<Palette> paletteList = new ArrayList<>(paletteCount);
        for (int i = 0; i < paletteCount; i++) {
            int colorNum = stream.readInt();
            byte[] paletteBytes = new byte[4 * colorNum];
            stream.readFully(paletteBytes);
            paletteList.add(Palette.fromBytes(paletteBytes, colorNum));
        }

        if (!paletteList.isEmpty()) {
            this.palettes = paletteList;
            this.palette = paletteList.get(0);
            this.activePaletteIndex = 0;
        }
    }

    @Override
    protected void writePalette(ImageOutputStream stream) throws IOException {
        stream.writeInt(palettes.size());
        for (Palette p : palettes) {
            stream.writeInt(p.size());
            stream.write(p.toBytes());
        }
    }

    /**
     * 获取当前激活的调色板索引
     */
    public int getActivePaletteIndex() {
        return activePaletteIndex;
    }
}
