package xyz.hooy.npkapi.util;

import xyz.hooy.npkapi.component.MemoryStream;
import xyz.hooy.npkapi.constant.ColorLinkTypes;

public final class ColorLinkUtils {

    public static void readColor(MemoryStream stream, ColorLinkTypes type, byte[] target, int offset) {
        byte[] bs;
        if (type == ColorLinkTypes.ARGB_8888) {
            bs = new byte[4];
            stream.read(bs);
            System.arraycopy(bs, 0, target, offset, bs.length);
            return;
        }
        bs = new byte[2];
        byte a = 0;
        byte r = 0;
        byte g = 0;
        byte b = 0;
        stream.read(bs);
        if (type == ColorLinkTypes.ARGB_1555) {
            b = (byte) ((bs[0] & 0x003F) << 3);
            g = (byte) ((((bs[1] & 0x0003) << 3) | ((bs[0] >> 5) & 0x0007)) << 3);
            r = (byte) (((bs[1] & 127) >> 2) << 3);
            a = (byte) ((bs[1] >> 7) == 0 ? 0 : 255);
        } else if (type == ColorLinkTypes.ARGB_4444) {
            b = (byte) ((bs[0] & 0x0F) << 4);
            g = (byte) (((bs[0] & 0xF0) >> 4) << 4);
            r = (byte) ((bs[1] & 0x0F) << 4);
            a = (byte) (((bs[1] & 0xF0) >> 4) << 4);
        }
        target[offset] = b;
        target[offset + 1] = g;
        target[offset + 2] = r;
        target[offset + 3] = a;
    }

    public static void writeColor(MemoryStream stream, byte[] data, ColorLinkTypes type) {
        if (type == ColorLinkTypes.ARGB_8888) {
            stream.write(data);
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
        stream.writeByte(left);
        stream.writeByte(right);
    }
}
