package xyz.hooy.npkapi.impl;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.io.IOException;

public class Version4ImgAccess extends ListableImgAccess {

    private final Version4Img version4Img;

    public Version4ImgAccess(Version4Img version4Img) {
        super(version4Img);
        this.version4Img = version4Img;
    }

    @Override
    protected void readHeader(ImageInputStream stream) throws IOException {
        super.readHeader(stream);
        int colorNum = stream.readInt();
        byte[] paletteBytes = new byte[4 * colorNum];
        stream.read(paletteBytes);
        Palette palette = new Palette();
        for (int i = 0; i < paletteBytes.length; i++) {
            Color color = new Color(paletteBytes[i], paletteBytes[i + 1], paletteBytes[i + 2], paletteBytes[i + 3]);
            palette.add(color);
        }
        version4Img.palette = palette;
    }

    @Override
    protected void writeHeader(ImageOutputStream stream) throws IOException {
        super.writeHeader(stream);
        Palette palette = version4Img.palette;
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
