package xyz.hooy.npkapi.util;

import xyz.hooy.npkapi.constant.ColorLinkTypes;

import java.nio.ByteBuffer;

public final class ColorUtils {

    public static void readColor(ByteBuffer buffer, ColorLinkTypes type, byte[] target, int offset) {
        byte[] bs;
        if (type == ColorLinkTypes.ARGB_8888) {
            bs = new byte[4];
            buffer.get(bs);
            System.arraycopy(bs, 0, target, offset, bs.length);
            return;
        }
        bs = new byte[2];
        byte a = 0;
        byte r = 0;
        byte g = 0;
        byte b = 0;
        buffer.get(bs);
        if (type == ColorLinkTypes.ARGB_1555) {
            a = (byte) (bs[1] >> 7);
            r = (byte) ((bs[1] >> 2) & 0x1f);
            g = (byte) ((bs[0] >> 5) | ((bs[1] & 3) << 3));
            b = (byte) (bs[0] & 0x1f);
            a = (byte) (a * 0xff);
            r = (byte) ((r << 3) | (r >> 2));
            g = (byte) ((g << 3) | (g >> 2));
            b = (byte) ((b << 3) | (b >> 2));
        } else if (type == ColorLinkTypes.ARGB_4444) {
            a = (byte) (bs[1] & 0xf0);
            r = (byte) ((bs[1] & 0xf) << 4);
            g = (byte) (bs[0] & 0xf0);
            b = (byte) ((bs[0] & 0xf) << 4);
        }
        target[offset] = b;
        target[offset + 1] = g;
        target[offset + 2] = r;
        target[offset + 3] = a;
    }

    public static void writeColor(ByteBuffer buffer, byte[] data, ColorLinkTypes type) {
        if (type == ColorLinkTypes.ARGB_8888) {
            buffer.put(data);
            return;
        }
        byte a = data[3];
        byte r = data[2];
        byte g = data[1];
        byte b = data[0];
        byte left = 0;
        byte right = 0;
        if (type == ColorLinkTypes.ARGB_1555) {
            a = (byte) (a >> 7);
            r = (byte) (r >> 3);
            g = (byte) (g >> 3);
            b = (byte) (b >> 3);
            left = (byte) (((g & 7) << 5) | b);
            right = (byte) ((a << 7) | (r << 2) | (g >> 3));
        } else if (type == ColorLinkTypes.ARGB_4444) {
            left = (byte) (g | (b >> 4));
            right = (byte) (a | (r >> 4));
        }
        buffer.put(left);
        buffer.put(right);
    }
}
