package xyz.hooy.npk.api.util;

import java.nio.charset.StandardCharsets;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-02
 */
public final class ByteUtils {

    /**
     * bytes <-> int - 小端模式
     */
    public static int bytesToInt(byte[] bytes) {
        return (bytes[0] & 0xFF)
                | ((bytes[1] & 0xFF) << 8)
                | ((bytes[2] & 0xFF) << 16)
                | ((bytes[3] & 0xFF) << 24);
    }

    public static byte[] intToBytes(int i) {
        return new byte[]{
                (byte) i,
                (byte) (i >>> 8),
                (byte) (i >>> 16),
                (byte) (i >>> 24)};
    }


    /**
     * bytes <-> String
     */
    public static String bytesToString(byte[] src) {
        return new String(src).trim();
    }

    public static byte[] stringToBytes(String src) {
        return src.getBytes(StandardCharsets.UTF_8);
    }


    public static byte[] mergeByteArrays(byte[]... arrays) {
        int sumLength = 0;
        for (byte[] array : arrays) {
            sumLength += array.length;
        }
        byte[] mergeArray = new byte[sumLength];
        int index = 0;
        for (byte[] array : arrays) {
            for (byte b : array) {
                mergeArray[index++] = b;
            }
        }
        return mergeArray;
    }
}
