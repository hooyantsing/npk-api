package xyz.hooy.npk.api.entity;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import xyz.hooy.npk.api.constant.ColorLinkTypes;
import xyz.hooy.npk.api.constant.CompressModes;
import xyz.hooy.npk.api.util.CompressUtils;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
@Data
public class TextureEntity {

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private BufferedImage image;

    private ColorLinkTypes type;
    private CompressModes compress = CompressModes.NONE;
    private Integer length = 0;
    private Integer width = 0;
    private Integer height = 0;
    private Integer x = 0;
    private Integer y = 0;
    private Integer frameWidth = 0;
    private Integer frameHeight = 0;
    private byte[] textureData = new byte[0];

    private Integer index;
    private ImgEntity parent;
    private TextureEntity target;

    public TextureEntity() {
    }

    public TextureEntity(ImgEntity parent) {
        this.parent = parent;
    }

    public boolean isOpen() {
        return Objects.nonNull(image);
    }

    public BufferedImage getPicture() {
        if (type == ColorLinkTypes.LINK) {
            return target.getPicture();
        }
        if (isOpen()) {
            return image;
        }
        image = parent.convertToBitmap(this);
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
        if (type == ColorLinkTypes.LINK) {
            length = 0;
            return;
        }
        if (!isOpen()) {
            return;
        }
        textureData = parent.convertToByte(this);
        if (textureData.length > 0 && compress.getValue() >= CompressModes.ZLIB.getValue()) {
            textureData = CompressUtils.zlibCompress(textureData);
        }
        length = textureData.length; //不压缩时，按原长度保存
    }
}
