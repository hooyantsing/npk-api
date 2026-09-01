package xyz.hooy.npkit;

import java.awt.image.BufferedImage;

public interface ImgWriter extends Img, Writer {

    void encodeFrame(BufferedImage image);
}
