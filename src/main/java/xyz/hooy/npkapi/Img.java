package xyz.hooy.npkapi;

import java.awt.*;
import java.awt.image.BufferedImage;

public interface Img extends Access {

    void setFrame(int index, int referenceIndex);

    void setFrame(int index, BufferedImage image);

    void setFrame(int index, int type, BufferedImage image);

    void setFrame(int index, BufferedImage image, Rectangle[] rectangles);

    void setFrame(int index, int type, BufferedImage image, Rectangle[] rectangles);

    void removeFrame(int index);

    int getFrameSize();

    BufferedImage getImage(int index);

    String getName();

    void setName(String imgName);

    int getVersion();
}
