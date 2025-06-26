package xyz.hooy.npkapi;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Version6ImgAccess extends ListableImgAccess {

    private final Version6Img version6Img;

    public Version6ImgAccess(Version6Img version6Img) {
        super(version6Img);
        this.version6Img = version6Img;
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
            version6Img.palettes = palettes;
            version6Img.setActivePalette(0);
        }
    }

    @Override
    protected void writeHeader(ImageOutputStream stream) throws IOException {
        super.writeHeader(stream);
        List<Palette> palettes = version6Img.palettes;
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
