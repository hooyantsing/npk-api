package xyz.hooy.npk.api.util;

import java.nio.charset.StandardCharsets;

public class NpkUtils {

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
}
