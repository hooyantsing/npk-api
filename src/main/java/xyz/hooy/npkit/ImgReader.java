package xyz.hooy.npkit;

import java.awt.image.BufferedImage;

public interface ImgReader extends Img, Reader {

    int countFrame();

    BufferedImage decodeFrame(int index);

    BufferedImage[] decodeAllFrames();
}
