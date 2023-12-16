package xyz.hooy.npk.api.constant;

public enum CompressModes {

    ZLIB(0x05),
    NON_ZLIB(0x06);

    private Integer value;

    CompressModes(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
