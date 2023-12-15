package xyz.hooy.npk.api.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

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

    /**
     * zlib 压缩
     */
    public static byte[] compressZlib(byte[] data) {
        Deflater deflater = new Deflater();
        deflater.reset();
        deflater.setInput(data);
        deflater.finish();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length)) {
            byte[] buffer = new byte[1024];
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            outputStream.close();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
