package xyz.hooy.npk.api.constant;

import lombok.Getter;

@Getter
public enum ImgVersions {

    VERSION_2(0x02),
    VERSION_4(0x04);

    private Integer value;

    ImgVersions(int value) {
        this.value = value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
