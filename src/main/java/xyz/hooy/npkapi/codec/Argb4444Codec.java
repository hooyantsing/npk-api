package xyz.hooy.npkapi.codec;

import xyz.hooy.npkapi.Frame;

import java.awt.image.BufferedImage;

/**
 * ARGB 4444 像素编解码器。
 * <p>
 * 每像素 16 位：4 bit Alpha + 4 bit Red + 4 bit Green + 4 bit Blue。
 * </p>
 */
public class Argb4444Codec extends Abstract16BitPixelCodec {

    private static final int[] BITMASKS = {0x0F00, 0x00F0, 0x000F, 0xF000};

    @Override
    protected int[] bitMasks() {
        return BITMASKS;
    }

    @Override
    public Frame encode(BufferedImage image, CodecContext context) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] argbData = new int[width * height];
        image.getRGB(0, 0, width, height, argbData, 0, width);

        byte[] data = new byte[argbData.length * 2];
        int i = 0;
        for (int pixel : argbData) {
            int a = ((pixel >> 24) & 0xFF) >> 4;
            int r = ((pixel >> 16) & 0xFF) >> 4;
            int g = ((pixel >> 8) & 0xFF) >> 4;
            int b = (pixel & 0xFF) >> 4;
            data[i++] = (byte) ((g << 4) | b);
            data[i++] = (byte) ((a << 4) | r);
        }
        return createFrame(Frame.TYPE_ARGB4444, image, data);
    }
}
