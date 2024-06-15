package xyz.hooy.npkapi.npk.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import xyz.hooy.npkapi.npk.constant.ColorLinkModes;
import xyz.hooy.npkapi.npk.constant.CompressModes;
import xyz.hooy.npkapi.npk.util.CompressUtils;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
@Getter
@Setter
public class Sprite {

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private BufferedImage image;

    private ColorLinkModes colorLink = ColorLinkModes.ARGB_8888;
    private CompressModes compress = CompressModes.NONE;
    private Integer length = 0;
    private Integer width = 0;
    private Integer height = 0;
    private Integer x = 0;
    private Integer y = 0;
    private Integer frameWidth = 0;
    private Integer frameHeight = 0;
    private byte[] data = new byte[0];

    private Integer index;
    private Album parent;
    private Sprite target;

    public Sprite() {
    }

    public Sprite(Album parent) {
        this.parent = parent;
    }

    public boolean isOpen() {
        return Objects.nonNull(image);
    }

    public BufferedImage getPicture() {
        if (colorLink == ColorLinkModes.LINK) {
            return target.getPicture();
        }
        if (isOpen()) {
            return image;
        }
        image = parent.convertToBufferedImage(this);
        return image;
    }

    public void setPicture(BufferedImage value) {
        image = value;
        if (value != null) {
            width = value.getWidth();
            height = value.getHeight();
        }
    }

    public void adjust() {
        if (colorLink == ColorLinkModes.LINK) {
            length = 0;
            return;
        }
        if (!isOpen()) {
            return;
        }
        data = parent.convertToByte(this);
        if (data.length > 0 && compress.getValue() >= CompressModes.ZLIB.getValue()) {
            data = CompressUtils.zlibCompress(data);
        }
        length = data.length; //不压缩时，按原长度保存
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Sprite that = (Sprite) o;
        return Objects.equals(index, that.index) && Objects.equals(parent, that.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, parent);
    }
}
