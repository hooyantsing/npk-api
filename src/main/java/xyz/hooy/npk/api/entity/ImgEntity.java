package xyz.hooy.npk.api.entity;

import lombok.Data;
import xyz.hooy.npk.api.constant.ImgVersions;
import xyz.hooy.npk.api.img.AbstractImgHandle;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

@Data
public class ImgEntity {

    private Integer length;
    private ImgVersions imgVersion;
    private Integer count;
    private byte[] imgData;

    private Integer offset;
    private Integer indexLength;
    private AbstractImgHandle imgHandle;
    private List<TextureEntity> textureEntities = new ArrayList<>();
    private ImgEntity target;

    public ImgEntity() {
        this.imgHandle = AbstractImgHandle.newInstance(this);
    }

    public BufferedImage convertToBitmap(TextureEntity textureEntity) {
        return imgHandle.convertToBufferedImage(textureEntity);
    }

    public byte[] convertToByte(TextureEntity textureEntity) {
        return imgHandle.convertToByte(textureEntity);
    }
}
