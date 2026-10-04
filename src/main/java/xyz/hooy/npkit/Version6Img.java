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
 * [V4 Palette] (主调色板，同 V4)
 * [Extra Palettes]
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
    }

    @Override
    public int version() {
        return 6;
    }

    // ===== 多调色板接口实现 =====

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
     * 获取当前激活的调色板索引
     */
    public int getActivePaletteIndex() {
        return activePaletteIndex;
    }

    // ===== Header 读写 =====

    @Override
    protected void readHeader(ImageInputStream stream) throws IOException {
        super.readHeader(stream); // 读取 V4 主调色板

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
    protected void writeHeader(ImageOutputStream stream) throws IOException {
        super.writeHeader(stream); // 写入 V4 主调色板

        stream.writeInt(palettes.size());
        for (Palette p : palettes) {
            stream.writeInt(p.size());
            stream.write(p.toBytes());
        }
    }
}
