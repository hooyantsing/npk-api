package xyz.hooy.npk.api.entity;

import lombok.Data;
import xyz.hooy.npk.api.constant.ColorLinkTypes;
import xyz.hooy.npk.api.constant.CompressModes;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
@Data
public class TextureEntity {

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
}
