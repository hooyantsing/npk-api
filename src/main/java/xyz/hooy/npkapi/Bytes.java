package xyz.hooy.npkapi;

import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;

/**
 * 字节流工具。
 * 提供 NPK/IMG 格式中常用的字节级操作：zlib 压缩解压、小端序读写。
 */
public final class Bytes {

    /**
     * 解压时的缓冲区大小
     */
    private static final int BUFFER_SIZE = 8192;

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
     * zlib 解压字节数据，输出大小以 {@code maxSize} 为上限。
     * <p>
     * 压缩数据的长度字段来自不可信文件，若不设上限，恶意构造的
     * 压缩炸弹可在解压时耗尽内存。超出上限或数据损坏时抛出 {@link NpkException}。
     * </p>
     *
     * @param bytes   zlib 压缩数据
     * @param maxSize 解压结果的字节数上限（须 &gt;= 0）
     * @return 解压后的字节数组（长度 &lt;= maxSize）
     * @throws NpkException 数据损坏、被截断或解压结果超过上限
     */
    public static byte[] decompress(byte[] bytes, int maxSize) {
        if (maxSize < 0) {
            throw new IllegalArgumentException("maxSize must be >= 0: " + maxSize);
        }
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(bytes);
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(maxSize, BUFFER_SIZE));
            byte[] buffer = new byte[BUFFER_SIZE];
            while (!inflater.finished()) {
                int inflated;
                try {
                    inflated = inflater.inflate(buffer);
                } catch (DataFormatException e) {
                    throw new NpkException("Failed to decompress data: invalid zlib format.", e);
                }
                if (inflated == 0) {
                    throw new NpkException("Failed to decompress data: "
                            + (inflater.needsDictionary() ? "preset dictionary required."
                            : "truncated zlib stream."));
                }
                out.write(buffer, 0, inflated);
                if (out.size() > maxSize) {
                    throw new NpkException("Decompressed data exceeds the declared size limit "
                            + maxSize + " bytes (possible corrupt or malicious data).");
                }
            }
            return out.toByteArray();
        } finally {
            inflater.end();
        }
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

    /**
     * 从字节数组指定偏移读取一个小端序 int32。
     */
    public static int readIntLE(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
    }

    /**
     * 向字节数组指定偏移写入一个小端序 int32。
     */
    public static void writeIntLE(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value & 0xFF);
        bytes[offset + 1] = (byte) ((value >> 8) & 0xFF);
        bytes[offset + 2] = (byte) ((value >> 16) & 0xFF);
        bytes[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }
}
