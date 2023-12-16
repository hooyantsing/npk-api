package xyz.hooy.npk.api.constant;

import lombok.Getter;

@Getter
public enum ColorLinkTypes {

    ARGB1555(0x0E),
    ARGB4444(0x0F),
    ARGB8888(0x10),
    LINK(0x11);

    private Integer value;

    ColorLinkTypes(int value) {
        this.value = value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public static ColorLinkTypes valueOf(int value) {
        for (ColorLinkTypes type : ColorLinkTypes.values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        return null;
    }
}
