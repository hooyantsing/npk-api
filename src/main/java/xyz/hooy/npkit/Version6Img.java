package xyz.hooy.npkit;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Version6Img extends Version4Img {

    private List<Palette> palettes = new ArrayList<>();

    @Override
    public int version() {
        return 6;
    }

    @Override
    public int getPaletteSourceCount() {
        return palettes.size();
    }

    @Override
    public Palette getPaletteSource(int index) {
        return palettes.get(index);
    }

    @Override
    public void activePaletteSource(int index) {
        this.palette = palettes.get(index);
    }

    @Override
    protected void readHeader(ImageInputStream stream) throws IOException {
        super.readHeader(stream);
        int paletteSize = stream.readInt();
        List<Palette> palettes = new ArrayList<>(paletteSize);
        for (int i = 0; i < paletteSize; i++) {
            int colorNum = stream.readInt();
            byte[] paletteBytes = new byte[4 * colorNum];
            stream.read(paletteBytes);
            Palette palette = new Palette();
            for (int j = 0; j < paletteBytes.length; j++) {
                Color color = new Color(paletteBytes[j], paletteBytes[j + 1], paletteBytes[j + 2], paletteBytes[j + 3]);
                palette.add(color);
            }
            palettes.add(palette);
        }
        if (!palettes.isEmpty()) {
            this.palettes = palettes;
            this.palette = palettes.get(0);
        }
    }

    @Override
    protected void writeHeader(ImageOutputStream stream) throws IOException {
        super.writeHeader(stream);
        stream.writeInt(palettes.size());
        for (Palette palette : palettes) {
            stream.writeInt(palette.size());
            for (int i = 0; i < palette.size(); i++) {
                Color color = palette.get(i);
                stream.writeByte(color.getRed());
                stream.writeByte(color.getGreen());
                stream.writeByte(color.getBlue());
                stream.writeByte(color.getAlpha());
            }
        }
    }
}
