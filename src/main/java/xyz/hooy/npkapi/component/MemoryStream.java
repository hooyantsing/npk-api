package xyz.hooy.npkapi.component;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

// AutoFlip + AutoResize
public class MemoryStream {

    public enum SeekOrigin {
        Begin, Current, End
    }

    @Getter
    private ByteBuffer buffer;

    private boolean writeFlip = true;

    private float expansionFactor = 1.5F;

    public MemoryStream() {
        resize(128);
    }

    public MemoryStream(int initialCapacity) {
        resize(initialCapacity);
    }

    public MemoryStream(int initialCapacity, float expansionFactor) {
        resize(initialCapacity);
        this.expansionFactor = expansionFactor;
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

    public String readString(int length) {
        autoReadFlip();
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return StringUtils.toEncodedString(bytes, StandardCharsets.UTF_8).trim();
    }

    public void writeByte(byte b) {
        autoResize(1);
        autoWriteFlip();
        buffer.put(b);
    }

    public void write(byte[] bytes) {
        autoResize(bytes.length);
        autoWriteFlip();
        buffer.put(bytes);
    }

    public void writeInt(int value) {
        autoResize(4);
        autoWriteFlip();
        buffer.putInt(value);
    }

    public void writeLong(long value) {
        autoResize(8);
        autoWriteFlip();
        buffer.putLong(value);
    }

    public void writeString(String value) {
        byte[] bytes = value.getBytes();
        autoResize(bytes.length);
        autoWriteFlip();
        buffer.put(bytes);
    }

    public int length() {
        autoReadFlip();
        ByteBuffer duplicate = buffer.duplicate();
        duplicate.position(0);
        return duplicate.remaining();
    }

    public int position() {
        return buffer.position();
    }

    public byte[] toArray() {
        autoReadFlip();
        ByteBuffer duplicate = buffer.duplicate();
        duplicate.position(0);
        int length = duplicate.remaining();
        byte[] bytes = new byte[length];
        duplicate.get(bytes);
        return bytes;
    }

    public void autoReadFlip() {
        if (writeFlip) {
            buffer.flip();
            writeFlip = false;
        }
    }

    public void autoWriteFlip() {
        if (!writeFlip) {
            buffer.flip();
            writeFlip = true;
        }
    }

    private void autoResize(int length) {
        autoWriteFlip();
        if (buffer.remaining() < length) {
            resize((int) (buffer.capacity() * expansionFactor));
            autoResize(length);
        }
    }

    private void resize(int newSize) {
        ByteBuffer newByteBuffer = ByteBuffer.allocate(newSize).order(ByteOrder.LITTLE_ENDIAN);
        if (Objects.nonNull(buffer)) {
            autoReadFlip();
            newByteBuffer.put(buffer);
            writeFlip = true;
        }
        buffer = newByteBuffer;
    }
}
