package xyz.hooy.npkapi.impl;

import java.awt.image.BufferedImage;

public class ImageFrame extends Frame {

    public final static int UNCOMPRESSED = 0x05;
    public final static int COMPRESSED = 0x06;

    protected BufferedImage image = null;

    protected int compressed = ImageFrame.COMPRESSED;
    protected int width = 0;
    protected int height = 0;
    protected int length = 0;
    protected int x = 0;
    protected int y = 0;
    protected int frameWidth = 0;
    protected int frameHeight = 0;
    protected byte[] rawData = null;

    public static boolean isCompressed(int compressed) {
        return compressed == COMPRESSED;
    }

    public boolean isCompressed() {
        return isCompressed(compressed);
    }
}
