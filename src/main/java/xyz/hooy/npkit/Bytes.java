package xyz.hooy.npkit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

/**
 * 字节流工具。
 * 提供 NPK/IMG 格式中常用的字节级操作：zlib 压缩解压、小端序转换。
 */
public final class Bytes {

    private Bytes() {
    }

    /**
     * zlib 压缩字节数据
     */
    public static byte[] compress(byte[] bytes) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(stream)) {
            deflaterOutputStream.write(bytes);
        } catch (IOException e) {
            throw new NpkException("Failed to compress data.", e);
        }
        return stream.toByteArray();
    }

    /**
     * zlib 解压字节数据
     */
    public static byte[] decompress(byte[] bytes) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (InflaterOutputStream inflaterOutputStream = new InflaterOutputStream(outputStream)) {
            inflaterOutputStream.write(bytes);
        } catch (IOException e) {
            throw new NpkException("Failed to decompress data.", e);
        }
        return outputStream.toByteArray();
    }

    /**
     * 将字节流按小端序两两合并为 short 数组（每两个字节组成一个 16 位字）。
     */
    public static short[] littleEndianShorts(byte[] bytes) {
        short[] shorts = new short[bytes.length / 2];
        for (int i = 0; i < shorts.length; i++) {
            byte low = bytes[i * 2];
            byte high = bytes[i * 2 + 1];
            shorts[i] = (short) ((high << 8) | (low & 0xFF));
        }
        return shorts;
    }
}
