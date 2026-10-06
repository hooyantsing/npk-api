package xyz.hooy.npkit;

import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
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
     * 跳过指定字节数。
     * 与 {@link ImageInputStream#skipBytes(int)} 不同，剩余字节不足时抛出
     * {@link EOFException} 而非静默少跳，避免截断文件导致后续解析错位。
     */
    public static void skipFully(ImageInputStream stream, int count) throws IOException {
        long remaining = count;
        while (remaining > 0) {
            long skipped = stream.skipBytes((int) Math.min(remaining, Integer.MAX_VALUE));
            if (skipped <= 0) {
                throw new EOFException("Failed to skip " + count + " bytes: stream exhausted.");
            }
            remaining -= skipped;
        }
    }

    /**
     * 将字节流按小端序两两合并为 short 数组（每两个字节组成一个 16 位字）。
     * 字节长度为奇数时，末尾多余的单个字节将被忽略。
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
