package xyz.hooy.npk.api.constant;

import lombok.Getter;

@Getter
public enum CompressModes {

    ZLIB(0x05),
    NONE(0x06);

    private Integer value;

    CompressModes(int value) {
        this.value = value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public static CompressModes valueOf(int value) {
        for (CompressModes compress : CompressModes.values()) {
            if (compress.getValue().equals(value)) {
                return compress;
            }
        }
        return null;
    }
}
