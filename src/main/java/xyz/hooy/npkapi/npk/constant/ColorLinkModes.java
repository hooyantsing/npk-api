package xyz.hooy.npkapi.npk.constant;

import lombok.Getter;

@Getter
public enum ColorLinkModes {

    ARGB_1555(0x0E),
    ARGB_4444(0x0F),
    ARGB_8888(0x10),
    LINK(0x11);

    private Integer value;

    ColorLinkModes(int value) {
        this.value = value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public static ColorLinkModes valueOf(int value) {
        for (ColorLinkModes type : ColorLinkModes.values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        return null;
    }
}
