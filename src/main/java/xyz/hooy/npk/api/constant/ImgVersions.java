package xyz.hooy.npk.api.constant;

import lombok.Getter;

@Getter
public enum ImgVersions {

    VERSION_1(0x01),
    VERSION_2(0x02),
    VERSION_4(0x04),
    VERSION_6(0x06);

    private Integer value;

    ImgVersions(int value) {
        this.value = value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
