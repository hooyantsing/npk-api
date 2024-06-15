package xyz.hooy.npkapi.npk.constant;

import lombok.Getter;

@Getter
public enum CompressModes {

    NONE(0x05),
    ZLIB(0x06);

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
