package xyz.hooy.npk.api.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.Inflater;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-02
 */
public final class ByteUtils {

    /**
     * bytes <-> Integer - 小端模式
     */
    public static Integer bytesToInt(byte[] bytes) {
        if (bytes.length == 4) {
            return (bytes[0] & 0xFF)
                    | ((bytes[1] & 0xFF) << 8)
                    | ((bytes[2] & 0xFF) << 16)
                    | ((bytes[3] & 0xFF) << 24);
        }
        return null;
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
        if (src.length > 0) {
            return new String(src);
        }
        return null;
    }

    public static byte[] stringToBytes(String src) {
        if (src.length() > 0) {
            return src.getBytes(StandardCharsets.UTF_8);
        }
        return null;
    }


    private static final byte[] DECRYPT_KEY = ("puchikon@neople dungeon and fighter " +
            "DNFDNFDNFDNFDNFDNFDNFDNFDNFDNF" +
            "DNFDNFDNFDNFDNFDNFDNFDNFDNFDNF" +
            "DNFDNFDNFDNFDNFDNFDNFDNFDNFDNF" +
            "DNFDNFDNFDNFDNFDNFDNFDNFDNFDNF" +
            "DNFDNFDNFDNFDNFDNFDNFDNFDNFDNF" +
            "DNFDNFDNFDNFDNFDNFDNFDNFDNFDNF" +
            "DNFDNFDNFDNFDNFDNFDNFDNFDNFDNF" +
            "DNFDNFDNF\0").getBytes(StandardCharsets.UTF_8);

    public static byte[] decryptImgName(byte[] encryptBytes) {
        if (encryptBytes.length == 256) {
            byte[] decryptBytes = new byte[256];
            for (int i = 0; i < encryptBytes.length; i++) {
                decryptBytes[i] = (byte) (encryptBytes[i] ^ DECRYPT_KEY[i]);
            }
            return decryptBytes;
        }
        return null;
    }

    public static byte[] encryptImgName(byte[] decryptBytes) {
        if (decryptBytes.length <= 256) {
            byte[] newBytes = new byte[256];
            System.arraycopy(decryptBytes, 0, newBytes, 0, decryptBytes.length);
            return decryptImgName(newBytes);
        }
        return null;
    }


    /**
     * zlib 解压
     */
    public static byte[] decompressZlib(byte[] data) {
        Inflater inflater = new Inflater();
        inflater.reset();
        inflater.setInput(data);
        try (ByteArrayOutputStream o = new ByteArrayOutputStream(data.length)) {
            byte[] buf = new byte[1024];
            while (!inflater.finished()) {
                int i = inflater.inflate(buf);
                o.write(buf, 0, i);
            }
            inflater.end();
            return o.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
