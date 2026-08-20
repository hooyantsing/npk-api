package xyz.hooy.npk.codec;

public class Frame {

    public final static int TYPE_INDEXED = 0x0E;
    public final static int TYPE_ARGB1555 = 0x0E;
    public final static int TYPE_ARGB4444 = 0x0F;
    public final static int TYPE_ARGB8888 = 0x10;
    public final static int TYPE_REFERENCE = 0x11;
    public final static int TYPE_FXT1 = 0x12;
    public final static int TYPE_FXT2 = 0x13;
    public final static int TYPE_FXT3 = 0x14;

    public final static int UNCOMPRESSED = 0x05;
    public final static int COMPRESSED = 0x06;

    protected int type;

    // ReferenceFrame
    protected int reference;

    // ImageFrame
    protected int compressed = COMPRESSED;
    protected int width = 0;
    protected int height = 0;
    protected int length = 0;
    protected int x = 0;
    protected int y = 0;
    protected int frameWidth = 0;
    protected int frameHeight = 0;
    protected byte[] rawData = null;

    public int getType() {
        return type;
    }

    public boolean isReference() {
        return isReferenceType(type);
    }

    public static boolean isReferenceType(int type) {
        return type == TYPE_REFERENCE;
    }

    public boolean isCompressed() {
        return isCompressed(compressed);
    }

    public static boolean isArgbType(int type) {
        return type == TYPE_ARGB1555 || type == TYPE_ARGB4444 || type == TYPE_ARGB8888;
    }

    public static boolean isCompressed(int compressed) {
        return compressed == COMPRESSED;
    }

    public boolean isArgb() {
        return isArgbType(type);
    }
}
