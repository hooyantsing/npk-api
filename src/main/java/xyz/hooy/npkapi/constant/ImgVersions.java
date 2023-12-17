package xyz.hooy.npkapi.constant;

import lombok.Getter;

@Getter
public enum ImgVersions {
    OTHER(0x00),
    VERSION_1(0x01),
    VERSION_2(0x02),
    VERSION_3(0x03),
    VERSION_4(0x04),
    VERSION_6(0x06);

    private Integer value;

    ImgVersions(int value) {
        this.value = value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public static ImgVersions valueOf(int value) {
        for (ImgVersions version : ImgVersions.values()) {
            if (version.getValue().equals(value)) {
                return version;
            }
        }
        return null;
    }
}
