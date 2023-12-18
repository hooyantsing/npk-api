package xyz.hooy.npkapi.component;

import org.apache.commons.lang3.StringUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class MemoryStream {

    public enum SeekOrigin {
        Begin, Current, End
    }

    private final ByteBuffer buffer;

    private boolean writeFlip = true;

    public MemoryStream(int capacity) {
        this.buffer = ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN);
    }

    public void seek(int position, SeekOrigin origin) {
        if (origin == SeekOrigin.Begin) {
            buffer.position(position);
        } else if (origin == SeekOrigin.Current) {
            buffer.position(buffer.position() + position);
        } else if (origin == SeekOrigin.End) {
            buffer.position(buffer.position() - position);
        }
    }

    public void read(byte[] bytes) {
        autoReadFlip();
        buffer.get(bytes);
    }

    public byte readByte() {
        autoReadFlip();
        return buffer.get();
    }

    public int readInt() {
        autoReadFlip();
        return buffer.getInt();
    }

    public long readLong() {
        autoReadFlip();
        return buffer.getLong();
    }

    public String readString() {
        autoReadFlip();
        return readString(16);
    }

    public String readPath() {
        autoReadFlip();
        return readString(256);
    }

    public String readString(int length) {
        autoReadFlip();
        byte[] bytes = new byte[length];
        read(bytes);
        return StringUtils.toEncodedString(bytes, StandardCharsets.UTF_8).trim();
    }

    public void writeByte(byte b) {
        autoWriteFlip();
        buffer.put(b);
    }

    public void write(byte[] bytes) {
        autoWriteFlip();
        buffer.put(bytes);
    }

    public void writeInt(int value) {
        autoWriteFlip();
        buffer.putInt(value);
    }

    public void writeLong(long value) {
        autoWriteFlip();
        buffer.putLong(value);
    }

    public int length() {
        return buffer.array().length;
    }

    public int position() {
        return buffer.position();
    }

    public byte[] toArray() {
        return buffer.array();
    }

    private void autoReadFlip() {
        if (writeFlip) {
            buffer.flip();
            writeFlip = false;
        }
    }

    private void autoWriteFlip() {
        if (!writeFlip) {
            buffer.flip();
            writeFlip = true;
        }
    }
}
