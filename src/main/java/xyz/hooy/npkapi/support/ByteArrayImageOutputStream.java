package xyz.hooy.npkapi.support;

import javax.imageio.stream.ImageOutputStreamImpl;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ByteArrayImageOutputStream extends ImageOutputStreamImpl {

    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    @Override
    public void write(int b) throws IOException {
        outputStream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        outputStream.write(b, off, len);
    }

    @Override
    public int read() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        throw new UnsupportedOperationException();
    }

    public byte[] toByteArray() {
        return outputStream.toByteArray();
    }
}
