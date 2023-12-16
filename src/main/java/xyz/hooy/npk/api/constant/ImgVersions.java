package xyz.hooy.npk.api.constant;

public enum ImgVersions {

    VERSION_2(0x02);

    private Integer value;

    ImgVersions(int value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
