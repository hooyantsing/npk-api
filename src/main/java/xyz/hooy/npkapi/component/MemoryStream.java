package xyz.hooy.npkapi.component;

import org.apache.commons.lang3.StringUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class MemoryStream {

    public enum SeekOrigin {
        Begin, Current, End
    }

    private ByteBuffer buffer;

    private boolean onlyRead = false;

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
        readFlip();
        buffer.get(bytes);
    }

    public byte readByte() {
        readFlip();
        return buffer.get();
    }

    public int readInt() {
        readFlip();
        return buffer.getInt();
    }

    public long readLong() {
        readFlip();
        return buffer.getLong();
    }

    public String readString() {
        readFlip();
        return readString(16);
    }

    public String readString(int length) {
        readFlip();
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return StringUtils.toEncodedString(bytes, StandardCharsets.UTF_8);
    }

    public void writeByte(byte b) {
        autoResize(1);
        buffer.put(b);
    }

    public void write(byte[] bytes) {
        autoResize(bytes.length);
        buffer.put(bytes);
    }

    public void writeInt(int value) {
        autoResize(4);
        buffer.putInt(value);
    }

    public void writeLong(long value) {
        autoResize(8);
        buffer.putLong(value);
    }

    public void writeString(String value) {
        byte[] bytes = value.getBytes();
        autoResize(bytes.length);
        buffer.put(bytes);
    }

    public int length() {
        ByteBuffer duplicate = buffer.duplicate();
        duplicate.flip();
        duplicate.position(0);
        return duplicate.remaining();
    }

    public byte[] toArray() {
        ByteBuffer duplicate = buffer.duplicate();
        duplicate.flip();
        duplicate.position(0);
        int length = duplicate.remaining();
        byte[] bytes = new byte[length];
        duplicate.get(bytes);
        return bytes;
    }

    public int position() {
        return buffer.position();
    }

    public ByteBuffer getOnlyReadBuffer() {
        readFlip();
        return buffer;
    }

    public ByteBuffer getWriteableBuffer() {
        return buffer;
    }

    private void readFlip() {
        if (!onlyRead) {
            buffer.flip();
            onlyRead = true;
        }
    }

    private void autoResize(int length) {
        if (buffer.remaining() < length) {
            resize((int) (buffer.capacity() * expansionFactor));
            autoResize(length);
        }
    }

    private void resize(int newSize) {
        ByteBuffer newByteBuffer = ByteBuffer.allocate(newSize).order(ByteOrder.LITTLE_ENDIAN);
        if (Objects.nonNull(buffer)) {
            buffer.flip();
            newByteBuffer.put(buffer);
        }
        buffer = newByteBuffer;
    }
}
