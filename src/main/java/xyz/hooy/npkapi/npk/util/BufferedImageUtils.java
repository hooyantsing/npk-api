package xyz.hooy.npkapi.npk.util;

import xyz.hooy.npkapi.component.MemoryStream;
import xyz.hooy.npkapi.npk.constant.ColorLinkModes;

import java.awt.image.BufferedImage;

public final class BufferedImageUtils {

    private BufferedImageUtils() {
    }

    public static byte[] toArray(BufferedImage bufferedImage, ColorLinkModes type) {
        byte[] data = toArray(bufferedImage);
        MemoryStream stream = new MemoryStream(data.length);
        for (int i = 0; i < data.length; i += 4) {
            byte[] temp = new byte[4];
            System.arraycopy(data, i, temp, 0, temp.length);
            ColorLinkUtils.writeColor(stream, temp, type);
        }
        return stream.toArray();
    }

    public static byte[] toArray(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        MemoryStream stream = new MemoryStream(width * height * 4);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = bufferedImage.getRGB(x, y);
                stream.writeInt(argb);
            }
        }
        return stream.toArray();
    }

    public static BufferedImage fromArray(byte[] data, int width, int height, ColorLinkModes type) {
        MemoryStream stream = new MemoryStream(data.length);
        stream.write(data);
        byte[] bufferedImageData = new byte[width * height * 4];
        for (int i = 0; i < bufferedImageData.length; i += 4) {
            ColorLinkUtils.readColor(stream, type, bufferedImageData, i);
        }
        return fromArray(bufferedImageData, width, height);
    }

    public static BufferedImage fromArray(byte[] data, int width, int height) {
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        MemoryStream stream = new MemoryStream(data.length);
        stream.write(data);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = stream.readInt();
                bufferedImage.setRGB(x, y, argb);
            }
        }
        return bufferedImage;
    }
}
