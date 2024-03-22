package xyz.hooy.npkapi.constant;

import lombok.Getter;

@Getter
public enum AlbumModes {

    OGG(0x00),
    VERSION_1(0x01),
    VERSION_2(0x02),
    VERSION_3(0x03),
    VERSION_4(0x04),
    VERSION_6(0x06);

    private Integer value;

    AlbumModes(int value) {
        this.value = value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public static AlbumModes valueOf(int value) {
        for (AlbumModes version : AlbumModes.values()) {
            if (version.getValue().equals(value)) {
                return version;
            }
        }
        return null;
    }
}
