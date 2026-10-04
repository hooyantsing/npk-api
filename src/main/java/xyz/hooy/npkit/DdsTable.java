package xyz.hooy.npkit;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DDS 纹理表。
 * <p>
 * IMG V5 格式使用 DDS 纹理表存储共享的 DDS 图像数据，
 * 多个帧可以通过索引引用同一张 DDS 纹理的不同裁剪区域，
 * 从而节省存储空间。
 * </p>
 */
public class DdsTable {

    private final Map<Integer, DDS> ddsTable = new LinkedHashMap<>();

    /**
     * 根据索引获取 DDS 条目
     */
    public DDS get(int index) {
        return ddsTable.get(index);
    }

    /**
     * 添加 DDS 条目。如果 index < 0，自动分配下一个可用索引。
     */
    public void put(DDS dds) {
        if (dds.getIndex() < 0) {
            dds.setIndex(nextIndex());
        }
        ddsTable.put(dds.getIndex(), dds);
    }

    /**
     * 移除指定索引的 DDS 条目
     */
    public void remove(int index) {
        ddsTable.remove(index);
    }

    /**
     * DDS 条目数量
     */
    public int size() {
        return ddsTable.size();
    }

    /**
     * 获取所有 DDS 条目（按插入顺序）
     */
    public Collection<DDS> values() {
        return ddsTable.values();
    }

    private int nextIndex() {
        int max = -1;
        for (Integer i : ddsTable.keySet()) {
            max = Math.max(max, i);
        }
        return max + 1;
    }

    /**
     * DDS 纹理条目数据模型
     */
    public static class DDS {

        private int title = 1;
        private int pixelFormat = Frame.TYPE_FXT1;
        private int index = -1;
        private int fullLength;
        private int length;
        private int width;
        private int height;
        private byte[] rawData;

        public DDS() {
        }

        @Override
        public String toString() {
            return "DDS{index=" + index + ", " + width + "x" + height
                    + ", pixelFormat=0x" + Integer.toHexString(pixelFormat) + "}";
        }

        public int getTitle() {
            return title;
        }

        public void setTitle(int title) {
            this.title = title;
        }

        public int getPixelFormat() {
            return pixelFormat;
        }

        public void setPixelFormat(int pixelFormat) {
            this.pixelFormat = pixelFormat;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public int getFullLength() {
            return fullLength;
        }

        public void setFullLength(int fullLength) {
            this.fullLength = fullLength;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public byte[] getRawData() {
            return rawData;
        }

        public void setRawData(byte[] rawData) {
            this.rawData = rawData;
        }
    }
}
