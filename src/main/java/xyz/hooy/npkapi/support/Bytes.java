package xyz.hooy.npkapi.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

public final class Bytes {

    private Bytes() {
        // not instance
    }

    public static byte[] decompress(byte[] bytes) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (InflaterOutputStream inflaterOutputStream = new InflaterOutputStream(outputStream)) {
            inflaterOutputStream.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outputStream.toByteArray();
    }

    public static byte[] compress(byte[] bytes) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(outputStream)) {
            deflaterOutputStream.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outputStream.toByteArray();
    }

    public static short[] shortsMergedFrom(byte[] bytes) {
        int i = 0;
        int index = 0;
        short[] merged = new short[bytes.length / 2];
        while (i < merged.length) {
            byte right = bytes[index++];
            byte left = bytes[index++];
            merged[i++] = (short) ((left << 8) | (right & 0xFF));
        }
        return merged;
    }
}
