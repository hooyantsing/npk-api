package xyz.hooy.npkapi;

public abstract class Frame {

    public final static int TYPE_INDEXED = 0x0E;
    public final static int TYPE_ARGB1555 = 0x0E;
    public final static int TYPE_ARGB4444 = 0x0F;
    public final static int TYPE_ARGB8888 = 0x10;
    public final static int TYPE_REFERENCE = 0x11;
    public final static int TYPE_FXT1 = 0x12;
    public final static int TYPE_FXT2 = 0x13;
    public final static int TYPE_FXT3 = 0x14;

    protected int type;

    public int getType() {
        return type;
    }

    public boolean isReference() {
        return this instanceof ReferenceFrame && isReferenceType(type);
    }

    public boolean isArgb() {
        return this instanceof ImageFrame && isArgbType(type);
    }

    public static boolean isReferenceType(int type) {
        return type == TYPE_REFERENCE;
    }

    public static boolean isArgbType(int type) {
        return type == TYPE_ARGB1555 || type == TYPE_ARGB4444 || type == TYPE_ARGB8888;
    }

    public static boolean isIndexed(int type) {
        return type == TYPE_INDEXED;
    }

    public static boolean isFxtType(int type) {
        return type == TYPE_FXT1 || type == TYPE_FXT2 || type == TYPE_FXT3;
    }

    public boolean isIndexed() {
        return this instanceof ImageFrame && isIndexed(type);
    }

    public boolean isFxt() {
        return this instanceof DdsImageFrame && isFxtType(type);
    }
}
