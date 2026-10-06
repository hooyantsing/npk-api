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
 * V6 在 V4 的基础上支持多个调色板，可通过 {@link #activatePaletteSource(int)}
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

    private List<PaletteSource> palettes = new ArrayList<>();
    private int activePaletteIndex;

    public Version6Img() {
        super();
        // 初始化一个空调色板方案，使新建的 V6 可直接 addImage；
        // 从文件读取时 readPalette 会覆盖此列表
        this.palettes.add(new PaletteSource());
        this.palette = this.palettes.get(0);
        this.activePaletteIndex = 0;
    }

    @Override
    public int getVersion() {
        return 6;
    }

    @Override
    public int getPaletteSourceCount() {
        return palettes.size();
    }

    @Override
    public void activatePaletteSource(int index) {
        if (index < 0 || index >= palettes.size()) {
            throw new IndexOutOfBoundsException(
                    "Palette index " + index + " out of bounds [0, " + palettes.size() + ")");
        }
        this.activePaletteIndex = index;
        this.palette = palettes.get(index);
    }

    @Override
    public PaletteSource getPaletteSource(int index) {
        if (index < 0 || index >= palettes.size()) {
            throw new IndexOutOfBoundsException(
                    "Palette index " + index + " out of bounds [0, " + palettes.size() + ")");
        }
        return palettes.get(index);
    }

    /**
     * 追加单帧（仅限单一调色板方案时使用）。
     * <p>
     * 多调色板状态下各调色板须锁步增长，本方法将抛出 {@link NpkException}，
     * 此时须改用 {@link #addPalettesImage(BufferedImage[])}。
     * </p>
     */
    @Override
    public void addImage(BufferedImage image, int frameType) {
        if (palettes.size() > 1) {
            throw new NpkException("This V6 IMG has " + palettes.size() + " palettes; use "
                    + "addPalettesImage(BufferedImage[]) to add a frame while keeping all palettes in sync. "
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
        if (paletteCount < 0) {
            throw new NpkException("Corrupt IMG v6: negative palette count " + paletteCount + ".");
        }
        List<PaletteSource> paletteList = new ArrayList<>(paletteCount);
        for (int i = 0; i < paletteCount; i++) {
            int colorNum = stream.readInt();
            if (colorNum < 0 || colorNum > PaletteSource.MAX_COLOR_SIZE) {
                throw new NpkException("Corrupt IMG v6: invalid color count " + colorNum
                        + " for palette #" + i + ".");
            }
            byte[] paletteBytes = new byte[4 * colorNum];
            stream.readFully(paletteBytes);
            paletteList.add(PaletteSource.fromBytes(paletteBytes, colorNum));
        }

        // 无条件替换：paletteCount==0 的文件写出时保持 0（往返保真），
        // 此时 palette 置 null，解码索引帧会在 IndexedCodec 处得到明确报错
        this.palettes = paletteList;
        this.palette = paletteList.isEmpty() ? null : paletteList.get(0);
        this.activePaletteIndex = 0;
    }

    @Override
    protected void writePalette(ImageOutputStream stream) throws IOException {
        stream.writeInt(palettes.size());
        for (PaletteSource p : palettes) {
            stream.writeInt(p.size());
            stream.write(p.toBytes());
        }
    }

    /**
     * 以「多调色板配色方案」的方式添加一帧，实现锁步机制：
     * <p>
     * 以参考图（palettesImage[0]）划分共享索引分区——同色像素归为同一索引；
     * 其余各图校验组内颜色一致后按相同索引顺序追加到对应调色板，
     * 保证所有调色板等量增长、索引→色槽映射跨调色板一致。
     * </p>
     * <p>
     * 所有图像必须尺寸完全一致且共享同一「形状分区」（仅颜色可不同）；
     * 既有调色板数量须与数组长度一致，否则抛出 {@link NpkException}。
     * </p>
     */
    @Override
    public void addPalettesImage(BufferedImage[] paletteImages) {
        validatePaletteImages(paletteImages);
        int paletteCount = paletteImages.length;
        ensurePaletteLayout(paletteCount);

        // 两阶段提交：第一阶段只收集与校验（不改动任何调色板），
        // 全部通过后第二阶段统一提交，任一步失败都不会留下半更新的调色板状态
        BufferedImage referenceImage = paletteImages[0];
        int oldSize = palettes.get(0).size();
        List<Color> referenceNewColors = new ArrayList<>();
        int[] indexes = assignColorIndexes(referenceImage, referenceNewColors);

        List<List<Color>> pendingColors = new ArrayList<>(paletteCount);
        pendingColors.add(referenceNewColors);
        for (int i = 1; i < paletteCount; i++) {
            pendingColors.add(syncPalette(i, paletteImages[i], indexes, oldSize, referenceNewColors.size()));
        }

        // 第二阶段：统一提交（256 色上限已在收集阶段预检，palette.add 仅作二次防线）
        for (int i = 0; i < paletteCount; i++) {
            PaletteSource palette = palettes.get(i);
            for (Color color : pendingColors.get(i)) {
                palette.add(color);
            }
        }

        frames.add(createIndexedFrame(referenceImage, indexes));
        invalidateRawData();
    }

    /**
     * 获取当前激活的调色板索引
     */
    @Override
    public int getActivePaletteSourceIndex() {
        return activePaletteIndex;
    }

    /**
     * 校验入参图像数组：非空、无 null 元素且所有图像尺寸完全一致。
     */
    private void validatePaletteImages(BufferedImage[] paletteImages) {
        if (paletteImages == null || paletteImages.length == 0) {
            throw new NpkException("paletteImages cannot be null or empty.");
        }
        for (int i = 0; i < paletteImages.length; i++) {
            if (paletteImages[i] == null) {
                throw new NpkException("paletteImages[" + i + "] is null.");
            }
        }
        BufferedImage referenceImage = paletteImages[0];
        int width = referenceImage.getWidth();
        int height = referenceImage.getHeight();
        for (int i = 1; i < paletteImages.length; i++) {
            if (paletteImages[i].getWidth() != width || paletteImages[i].getHeight() != height) {
                throw new NpkException("All images must share the same dimensions: image[0]="
                        + width + "x" + height + ", image[" + i + "]="
                        + paletteImages[i].getWidth() + "x" + paletteImages[i].getHeight());
            }
        }
    }

    /**
     * 确保调色板布局与图像数组长度匹配：所有调色板全空视为从零创作，
     * 按数组长度创建等量空调色板；否则既有调色板数量须与数组长度一致且等长。
     */
    private void ensurePaletteLayout(int paletteCount) {
        if (isAllPalettesEmpty()) {
            List<PaletteSource> newPalettes = new ArrayList<>(paletteCount);
            for (int i = 0; i < paletteCount; i++) {
                newPalettes.add(new PaletteSource());
            }
            this.palettes = newPalettes;
            this.activePaletteIndex = 0;
            this.palette = newPalettes.get(0);
        } else {
            if (palettes.size() != paletteCount) {
                throw new NpkException("paletteImages length (" + paletteCount + ") does not match the "
                        + "established palette count (" + palettes.size() + "). Once palettes are "
                        + "established, every addPalettesImage(BufferedImage[]) call must pass the same number of images.");
            }
            int referenceSize = palettes.get(0).size();
            for (int i = 1; i < palettes.size(); i++) {
                if (palettes.get(i).size() != referenceSize) {
                    throw new NpkException("Existing palettes have inconsistent sizes (palette[0]="
                            + referenceSize + ", palette[" + i + "]=" + palettes.get(i).size()
                            + "); multi-palette V6 requires all palettes to be equal-sized.");
                }
            }
        }
    }

    /**
     * 以参考图为所有调色板划分共享索引分区，返回每像素的索引流。
     * <p>
     * 同色像素归为同一索引；既有颜色复用其索引以保证跨帧一致。
     * 新颜色不直接追加参考调色板，而是暂存到 {@code newColors} 交由调用方统一提交
     * （两阶段提交，校验失败时调色板不被部分污染）。
     * </p>
     */
    private int[] assignColorIndexes(BufferedImage referenceImage, List<Color> newColors) {
        int width = referenceImage.getWidth();
        int height = referenceImage.getHeight();
        int[] referenceArgb = referenceImage.getRGB(0, 0, width, height, null, 0, width);
        Map<Integer, Integer> argbToIndex = new HashMap<>();
        PaletteSource referencePalette = palettes.get(0);
        int oldSize = referencePalette.size();
        for (int i = 0; i < oldSize; i++) {
            argbToIndex.put(referencePalette.get(i).getRGB(), i); // 复用既有颜色索引，保证跨帧一致
        }
        int[] indexes = new int[referenceArgb.length];
        for (int pixel = 0; pixel < referenceArgb.length; pixel++) {
            int argb = referenceArgb[pixel];
            Integer colorIndex = argbToIndex.get(argb);
            if (colorIndex == null) {
                if (oldSize + newColors.size() >= PaletteSource.MAX_COLOR_SIZE) {
                    throw new NpkException("Palette overflow: cannot exceed "
                            + PaletteSource.MAX_COLOR_SIZE + " colors while adding a multi-palette frame.");
                }
                newColors.add(new Color(argb, true));
                colorIndex = oldSize + newColors.size() - 1;
                argbToIndex.put(argb, colorIndex);
            }
            indexes[pixel] = colorIndex;
        }
        return indexes;
    }

    /**
     * 按参考分区校验指定图像并收集其待追加的新颜色。
     * <p>
     * 既有索引（小于 oldSize）处颜色须与该调色板既有颜色一致；
     * 新索引分区组内颜色须一致。新颜色不直接追加调色板，而是按索引顺序
     * 收集为返回值交由调用方统一提交（两阶段提交）。
     * </p>
     */
    private List<Color> syncPalette(int paletteIndex, BufferedImage image,
                                    int[] indexes, int oldSize, int newColorCount) {
        PaletteSource palette = palettes.get(paletteIndex);
        int width = image.getWidth();
        int[] argb = image.getRGB(0, 0, width, image.getHeight(), null, 0, width);
        int newSize = oldSize + newColorCount;
        int[] slotColor = new int[newSize];
        boolean[] slotSet = new boolean[newSize];
        for (int pixel = 0; pixel < indexes.length; pixel++) {
            int colorIndex = indexes[pixel];
            int color = argb[pixel];
            if (colorIndex < oldSize) {
                if (palette.get(colorIndex).getRGB() != color) {
                    throw new NpkException("image[" + paletteIndex + "] conflicts with the established "
                            + "index mapping at pixel (" + (pixel % width) + "," + (pixel / width) + "), index " + colorIndex
                            + ". All frames must share one index-to-region mapping.");
                }
            } else {
                if (slotSet[colorIndex] && slotColor[colorIndex] != color) {
                    throw new NpkException("image[" + paletteIndex + "] has different colors within the same "
                            + "reference region (index " + colorIndex + "). All images must share the same "
                            + "shape (region partition); only colors may differ.");
                }
                slotColor[colorIndex] = color;
                slotSet[colorIndex] = true;
            }
        }
        List<Color> appended = new ArrayList<>(newSize - oldSize);
        for (int colorIndex = oldSize; colorIndex < newSize; colorIndex++) {
            appended.add(new Color(slotColor[colorIndex], true));
        }
        return appended;
    }

    /**
     * 由索引流构建 V6 索引色帧（宽高与绘制偏移取自参考图，帧域尺寸等于图像尺寸）。
     */
    private Frame createIndexedFrame(BufferedImage referenceImage, int[] indexes) {
        byte[] data = new byte[indexes.length];
        for (int pixel = 0; pixel < indexes.length; pixel++) {
            data[pixel] = (byte) indexes[pixel];
        }
        Frame frame = new Frame(Frame.TYPE_INDEXED);
        frame.setWidth(referenceImage.getWidth());
        frame.setHeight(referenceImage.getHeight());
        frame.setX(referenceImage.getMinX());
        frame.setY(referenceImage.getMinY());
        frame.setFrameWidth(referenceImage.getWidth());
        frame.setFrameHeight(referenceImage.getHeight());
        frame.compressAndSetData(data);
        return frame;
    }

    /**
     * 判断当前所有调色板是否均为空（用于区分「从零创作」与「扩充既有调色板」）。
     */
    private boolean isAllPalettesEmpty() {
        for (PaletteSource p : palettes) {
            if (p.size() != 0) {
                return false;
            }
        }
        return true;
    }
}
