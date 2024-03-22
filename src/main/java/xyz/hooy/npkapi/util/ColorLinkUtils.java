package xyz.hooy.npkapi.util;

import xyz.hooy.npkapi.component.MemoryStream;
import xyz.hooy.npkapi.constant.ColorLinkModes;

public final class ColorLinkUtils {

    private ColorLinkUtils() {
    }

    public static void readColor(MemoryStream stream, ColorLinkModes type, byte[] target, int offset) {
        byte[] bs;
        if (type == ColorLinkModes.ARGB_8888) {
            bs = new byte[4];
            stream.read(bs);
            System.arraycopy(bs, 0, target, offset, bs.length);
            return;
        }
        bs = new byte[2];
        byte a, r, g, b;
        stream.read(bs);
        if (type == ColorLinkModes.ARGB_1555) {
            short argb1555 = (short) ((bs[1] & 0xFF) << 8 | (bs[0] & 0xFF));
            a = (byte) ((argb1555 >> 15 & 0x1) * 255);
            r = (byte) ((argb1555 >> 10 & 0x1F) << 3);
            g = (byte) ((argb1555 >> 5 & 0x1F) << 3);
            b = (byte) ((argb1555 & 0x1F) << 3);
        } else if (type == ColorLinkModes.ARGB_4444) {
            short argb4444 = (short) ((bs[1] & 0xFF) << 8 | (bs[0] & 0xFF));
            a = (byte) (((argb4444 >> 12) & 0xF) << 4);
            r = (byte) (((argb4444 >> 8) & 0xF) << 4);
            g = (byte) (((argb4444 >> 4) & 0xF) << 4);
            b = (byte) ((argb4444 & 0xF) << 4);
        } else {
            throw new UnsupportedOperationException(String.format("The current color is not supported %s", type));
        }
        target[offset] = b;
        target[offset + 1] = g;
        target[offset + 2] = r;
        target[offset + 3] = a;
    }

    public static void writeColor(MemoryStream stream, byte[] data, ColorLinkModes type) {
        if (type == ColorLinkModes.ARGB_8888) {
            stream.write(data);
            return;
        }
        byte a = data[3];
        byte r = data[2];
        byte g = data[1];
        byte b = data[0];
        byte left, right;
        if (type == ColorLinkModes.ARGB_1555) {
            short argb1555 = (short) (((a >> 7 & 0xFF) << 15) | ((r >> 3 & 0xFF) << 10) | ((g >> 3 & 0xFF) << 5) | (b >> 3 & 0xFF));
            left = (byte) (argb1555 & 0xFF);
            right = (byte) (argb1555 >> 8 & 0xFF);
        } else if (type == ColorLinkModes.ARGB_4444) {
            left = (byte) (g | (b >> 4 & 0xFF));
            right = (byte) (a | (r >> 4 & 0xFF));
        } else {
            throw new UnsupportedOperationException(String.format("The current color is not supported %s", type));
        }
        stream.writeByte(left);
        stream.writeByte(right);
    }
}
