package xyz.hooy.npkapi.util;

import xyz.hooy.npkapi.constant.ColorLinkTypes;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class BufferedImageUtils {

    public static byte[] toArray(BufferedImage bufferedImage, ColorLinkTypes type) {
        byte[] data = toArray(bufferedImage);
        ByteBuffer buffer = ByteBuffer.allocate(data.length).order(ByteOrder.LITTLE_ENDIAN);
        ;
        for (int i = 0; i < data.length; i += 4) {
            byte[] temp = new byte[4];
            System.arraycopy(data, i, temp, 0, temp.length);
            ColorUtils.writeColor(buffer, temp, type);
        }
        return buffer.array();
    }

    public static byte[] toArray(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        ByteBuffer buffer = ByteBuffer.allocate(width * height * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = bufferedImage.getRGB(x, y);
                buffer.putInt(argb);
            }
        }
        return buffer.array();
    }

    public static BufferedImage fromArray(byte[] data, int width, int height, ColorLinkTypes type) {
        ByteBuffer buffer = ByteBuffer.allocate(data.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(data);
        buffer.flip();
        byte[] bufferedImageData = new byte[width * height * 4];
        for (int i = 0; i < bufferedImageData.length; i += 4) {
            ColorUtils.readColor(buffer, type, bufferedImageData, i);
        }
        return fromArray(bufferedImageData, width, height);
    }

    public static BufferedImage fromArray(byte[] data, int width, int height) {
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        ByteBuffer buffer = ByteBuffer.allocate(data.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(data);
        buffer.flip();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = buffer.getInt();
                bufferedImage.setRGB(x, y, argb);
            }
        }
        return bufferedImage;
    }
}
