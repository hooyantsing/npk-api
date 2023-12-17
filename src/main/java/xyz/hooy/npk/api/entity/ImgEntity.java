package xyz.hooy.npk.api.entity;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import xyz.hooy.npk.api.constant.CompressModes;
import xyz.hooy.npk.api.constant.ImgVersions;
import xyz.hooy.npk.api.img.AbstractImgHandle;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Data
public class ImgEntity {

    private Integer length;
    private ImgVersions imgVersion;
    private Integer count;
    private byte[] imgData;

    private String path;
    private Integer offset;
    private Long indexLength;
    private AbstractImgHandle imgHandle;
    private List<TextureEntity> textureEntities = new ArrayList<>();
    private ImgEntity target;

    public ImgEntity() {
        this.imgHandle = AbstractImgHandle.newInstance(this);
    }

    public ImgEntity(BufferedImage[] array) {
        this();
        TextureEntity[] sprites = new TextureEntity[array.length];
        for (int i = 0; i < array.length; i++) {
            sprites[i] = new TextureEntity(this);
            sprites[i].setIndex(i);
            sprites[i].setPicture(array[i]);
            sprites[i].setCompress(CompressModes.ZLIB);
            sprites[i].setX(array[i].getWidth());
            sprites[i].setY(array[i].getHeight());
            sprites[i].setFrameWidth(array[i].getWidth());
            sprites[i].setFrameHeight(array[i].getHeight());
        }
        textureEntities.addAll(Arrays.asList(sprites));
        adjust();
    }

    public void initHandle(ByteBuffer buffer) {
        imgHandle = AbstractImgHandle.newInstance(this);
        if (Objects.nonNull(imgHandle) && Objects.nonNull(buffer)) {
            imgHandle.createFromBuffer(buffer);
        }
    }

    public void adjust() {
        if (Objects.nonNull(target)) {
            return;
        }
        adjustIndex();
        imgHandle.adjust();
    }

    public void adjustIndex() {
        for (int i = 0; i < textureEntities.size(); i++) {
            textureEntities.get(i).setIndex(i);
            textureEntities.get(i).setParent(this);
        }
    }

    public BufferedImage convertToBitmap(TextureEntity textureEntity) {
        return imgHandle.convertToBufferedImage(textureEntity);
    }

    public byte[] convertToByte(TextureEntity textureEntity) {
        return imgHandle.convertToByte(textureEntity);
    }

    public String getName() {
        return StringUtils.substringAfterLast(path, ".");
    }
}
