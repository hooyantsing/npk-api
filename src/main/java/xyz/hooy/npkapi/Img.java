package xyz.hooy.npkapi;

import java.awt.*;
import java.awt.image.BufferedImage;

public interface Img extends Access {

    void addFrame(int index, int referenceIndex);

    void addFrame(int index, BufferedImage image);

    void addFrame(int index, int type, BufferedImage image);

    void addFrame(int index, BufferedImage image, Rectangle[] rectangles);

    void addFrame(int index, int type, BufferedImage image, Rectangle[] rectangles);

    void removeFrame(int index);

    int getFrameSize();

    BufferedImage getImage(int index);

    String getName();

    void setName(String imgName);

    int getVersion();
}
