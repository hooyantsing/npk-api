package xyz.hooy.npkapi.npk.util;

import lombok.SneakyThrows;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
public final class CompressUtils {

    private CompressUtils() {
    }

    /**
     * zlib 解压
     */
    @SneakyThrows
    public static byte[] zlibDecompress(byte[] data) {
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
        }
    }

    /**
     * zlib 压缩
     */
    @SneakyThrows
    public static byte[] zlibCompress(byte[] data) {
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
            deflater.end();
            return outputStream.toByteArray();
        }
    }
}
