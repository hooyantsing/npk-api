package xyz.hooy.npkapi;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Palette {

    private final static int MAX_COLOR_SIZE = 255;

    private final List<Color> colors = new ArrayList<>(MAX_COLOR_SIZE);

    public Color get(int index) {
        return colors.get(index);
    }

    public void add(Color color) {
        colors.add(color);
    }

    public boolean contains(Color color) {
        return colors.contains(color);
    }

    public byte indexOf(Color color) {
        return (byte) colors.indexOf(color);
    }

    public int size() {
        return colors.size();
    }

    public int[] toArray() {
        int[] array = new int[colors.size()];
        for (int i = 0; i < colors.size(); i++) {
            Color color = colors.get(i);
            array[i] = color.getRGB();
        }
        return array;
    }
}
