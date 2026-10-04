package xyz.hooy.npkit;

import java.awt.*;
import java.awt.image.BufferedImage;

public interface Img {

    int version();

    String getName();

    void setName(String name);

    int getImageCount();

    BufferedImage getImage(int index);

    void addImage(BufferedImage image, int frameType);

    void addImage(int referenceIndex);

    void addImage(BufferedImage ddsImage, Rectangle[] subviews);

    void addImage(BufferedImage[] palettesImage);

    int getDdsSourceCount();

    BufferedImage getDdsSource(int index);

    int getPaletteSourceCount();

    Palette getPaletteSource(int index);

    void activePaletteSource(int index);
}
