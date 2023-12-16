package xyz.hooy.npk.api.constant;

public enum ColorLinkTypes {

    ARGB1555(0x0E),
    ARGB4444(0x0F),
    ARGB8888(0x10),
    LINK(0x11);

    private Integer value;

    ColorLinkTypes(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
