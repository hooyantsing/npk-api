package xyz.hooy.npkit;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private int activePaletteIndex;

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
    public void addImage(BufferedImage image, int frameType) {
        if (palettes.size() > 1) {
            throw new NpkException("This V6 IMG has " + palettes.size() + " palettes; use "
                    + "addImage(BufferedImage[]) to add a frame while keeping all palettes in sync. "
                    + "Single-image addImage is only allowed when there is exactly one palette.");
        }
        super.addImage(image, frameType);
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
     * 以「多调色板配色方案」的方式添加一帧。
     * <p>
     * 用于 V6 的多调色板场景（装备染色、队伍区分等）：数组长度即调色板方案数，
     * 每个元素是同一帧在对应配色方案下的图像。方法会：
     * <ol>
     *   <li>以 {@code paletteImages[0]} 为参考，按「同色像素归为同一索引」划分共享索引分区，
     *       生成唯一的帧索引流（V6 帧不记录调色板序号，所有帧共用一套索引映射）；</li>
     *   <li>校验其它每张图在参考分区组内 ARGB 必须一致（形状/分区一致，仅颜色可不同），
     *       否则抛 {@link NpkException}；</li>
     *   <li>把每张图各界定区域的颜色按相同索引顺序追加进对应调色板，
     *       使所有调色板等量增长、始终等长。</li>
     * </ol>
     * 从零创作时（当前所有调色板为空）会按数组长度自动创建对应数量的调色板，
     * 不要求预先匹配默认的单调色板；一旦调色板已建立（含颜色），
     * 后续每次调用的数组长度必须与既有调色板数一致。
     * </p>
     *
     * @param paletteImages 同一帧在各配色方案下的图像，长度等于调色板方案数，尺寸须完全一致
     * @throws NpkException 入参非法、尺寸不一致、或某图在参考分区组内颜色不一致
     */
    public void addImage(BufferedImage[] paletteImages) {
        if (paletteImages == null || paletteImages.length == 0) {
            throw new NpkException("paletteImages cannot be null or empty.");
        }
        int n = paletteImages.length;
        for (int i = 0; i < n; i++) {
            if (paletteImages[i] == null) {
                throw new NpkException("paletteImages[" + i + "] is null.");
            }
        }

        BufferedImage ref = paletteImages[0];
        int w = ref.getWidth();
        int h = ref.getHeight();
        for (int i = 1; i < n; i++) {
            if (paletteImages[i].getWidth() != w || paletteImages[i].getHeight() != h) {
                throw new NpkException("All images must share the same dimensions: image[0]="
                        + w + "x" + h + ", image[" + i + "]="
                        + paletteImages[i].getWidth() + "x" + paletteImages[i].getHeight());
            }
        }

        // 调色板数量由数组长度决定：全空视为从零创作，直接创建 n 个；否则须与既有数量一致且等长
        if (isAllPalettesEmpty()) {
            List<Palette> newPalettes = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                newPalettes.add(new Palette());
            }
            this.palettes = newPalettes;
            this.activePaletteIndex = 0;
            this.palette = newPalettes.get(0);
        } else {
            if (palettes.size() != n) {
                throw new NpkException("paletteImages length (" + n + ") does not match the "
                        + "established palette count (" + palettes.size() + "). Once palettes are "
                        + "established, every addImage(BufferedImage[]) call must pass the same number of images.");
            }
            int size0 = palettes.get(0).size();
            for (int i = 1; i < palettes.size(); i++) {
                if (palettes.get(i).size() != size0) {
                    throw new NpkException("Existing palettes have inconsistent sizes (palette[0]="
                            + size0 + ", palette[" + i + "]=" + palettes.get(i).size()
                            + "); multi-palette V6 requires all palettes to be equal-sized.");
                }
            }
        }

        int oldSize = palettes.get(0).size();

        // 以参考图划分共享索引分区：同色像素同一索引，新颜色追加到 palette[0]
        int[] refArgb = ref.getRGB(0, 0, w, h, null, 0, w);
        int[] index = new int[w * h];
        Map<Integer, Integer> argbToIndex = new HashMap<>();
        Palette p0 = palettes.get(0);
        for (int g = 0; g < oldSize; g++) {
            argbToIndex.put(p0.get(g).getRGB(), g); // 复用既有颜色索引，保证跨帧一致
        }
        for (int p = 0; p < index.length; p++) {
            int argb = refArgb[p];
            Integer g = argbToIndex.get(argb);
            if (g == null) {
                p0.add(new Color(argb, true));
                g = p0.size() - 1;
                argbToIndex.put(argb, g);
            }
            index[p] = g;
        }
        int newSize = p0.size();

        // 其余调色板按相同索引顺序填色，并校验「组内一致」与「与既有映射一致」
        for (int i = 1; i < n; i++) {
            Palette pi = palettes.get(i);
            int[] argbi = paletteImages[i].getRGB(0, 0, w, h, null, 0, w);
            int[] slotColor = new int[newSize];
            boolean[] slotSet = new boolean[newSize];
            for (int p = 0; p < index.length; p++) {
                int g = index[p];
                int c = argbi[p];
                if (g < oldSize) {
                    if (pi.get(g).getRGB() != c) {
                        throw new NpkException("image[" + i + "] conflicts with the established "
                                + "index mapping at pixel (" + (p % w) + "," + (p / w) + "), index " + g
                                + ". All frames must share one index-to-region mapping.");
                    }
                } else {
                    if (slotSet[g] && slotColor[g] != c) {
                        throw new NpkException("image[" + i + "] has different colors within the same "
                                + "reference region (index " + g + "). All images must share the same "
                                + "shape (region partition); only colors may differ.");
                    }
                    slotColor[g] = c;
                    slotSet[g] = true;
                }
            }
            for (int g = oldSize; g < newSize; g++) {
                pi.add(new Color(slotColor[g], true));
            }
        }

        // 生成帧：索引流来自参考图分区
        byte[] data = new byte[index.length];
        for (int p = 0; p < index.length; p++) {
            data[p] = (byte) index[p];
        }
        Frame frame = new Frame(Frame.TYPE_INDEXED);
        frame.setWidth(w);
        frame.setHeight(h);
        frame.setX(ref.getMinX());
        frame.setY(ref.getMinY());
        frame.setFrameWidth(w);
        frame.setFrameHeight(h);
        frame.compressAndSetData(data);
        frames.add(frame);
        invalidateRawData();
    }

    /**
     * 获取当前激活的调色板索引
     */
    public int getActivePaletteIndex() {
        return activePaletteIndex;
    }

    /**
     * 判断当前所有调色板是否均为空（用于区分「从零创作」与「扩充既有调色板」）。
     */
    private boolean isAllPalettesEmpty() {
        for (Palette p : palettes) {
            if (p.size() != 0) {
                return false;
            }
        }
        return true;
    }
}
