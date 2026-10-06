package xyz.hooy.npkapi;

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
 *
 * @see DdsSource
 */
public class DdsTable {

    private final Map<Integer, DdsSource> ddsTable = new LinkedHashMap<>();

    /**
     * 根据索引获取 DDS 纹理源
     */
    public DdsSource get(int index) {
        return ddsTable.get(index);
    }

    /**
     * 添加 DDS 纹理源。如果 {@code index < 0}，自动分配下一个可用索引。
     *
     * @param ddsSource 纹理源（非 null）
     * @throws NpkException 纹理源为 null，或其索引已被表中既有纹理占用
     */
    public void put(DdsSource ddsSource) {
        if (ddsSource == null) {
            throw new NpkException("DdsSource cannot be null.");
        }
        if (ddsSource.getIndex() < 0) {
            ddsSource.setIndex(nextIndex());
        } else if (ddsTable.containsKey(ddsSource.getIndex())) {
            throw new NpkException("Duplicate DDS index: " + ddsSource.getIndex()
                    + " is already occupied in the DDS table.");
        }
        ddsTable.put(ddsSource.getIndex(), ddsSource);
    }

    /**
     * 移除指定索引的 DDS 纹理源
     */
    public void remove(int index) {
        ddsTable.remove(index);
    }

    /**
     * DDS 纹理源数量
     */
    public int size() {
        return ddsTable.size();
    }

    /**
     * 获取所有 DDS 纹理源（按插入顺序）
     */
    public Collection<DdsSource> values() {
        return ddsTable.values();
    }

    private int nextIndex() {
        int max = -1;
        for (Integer i : ddsTable.keySet()) {
            max = Math.max(max, i);
        }
        return max + 1;
    }
}
