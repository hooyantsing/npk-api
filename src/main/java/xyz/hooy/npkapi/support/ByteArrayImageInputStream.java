package xyz.hooy.npkapi.support;

import javax.imageio.stream.ImageInputStreamImpl;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ByteArrayImageInputStream extends ImageInputStreamImpl {

    private final ByteArrayInputStream inputStream;

    public ByteArrayImageInputStream(byte[] bytes) {
        this.inputStream = new ByteArrayInputStream(bytes);
    }

    @Override
    public int read() throws IOException {
        return inputStream.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return inputStream.read(b, off, len);
    }
}
