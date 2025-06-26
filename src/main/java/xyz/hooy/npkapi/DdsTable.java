package xyz.hooy.npkapi;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class DdsTable {

    private final Map<Integer, DDS> ddsTable = new LinkedHashMap<>();

    public DDS get(int index) {
        return ddsTable.get(index);
    }

    public void put(DDS dds) {
        if (dds.index < 0) {
            dds.index = maxIndex() + 1;
        }
        ddsTable.put(dds.index, dds);
    }

    public void remove(int index) {
        ddsTable.remove(index);
    }

    public int size() {
        return ddsTable.size();
    }

    public Collection<DDS> values() {
        return ddsTable.values();
    }

    private int maxIndex() {
        int max = 0;
        for (Integer i : ddsTable.keySet()) {
            max = Math.max(max, i);
        }
        return max;
    }

    public static class DDS {

        int title = 1; // default 1
        int pixelFormat = Frame.TYPE_FXT1;
        int index = -1;
        int fullLength = 0;
        int length = 0;
        int width = 0;
        int height = 0;
        byte[] rawData = null;
    }
}
