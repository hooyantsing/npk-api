package xyz.hooy.npkapi.entity;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import xyz.hooy.npkapi.component.MemoryStream;
import xyz.hooy.npkapi.constant.CompressModes;
import xyz.hooy.npkapi.constant.ImgVersions;
import xyz.hooy.npkapi.img.AbstractImgHandle;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class ImgEntity {

    private Integer length;
    private ImgVersions imgVersion = ImgVersions.VERSION_2;
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

    public void initHandle(MemoryStream stream) {
        imgHandle = AbstractImgHandle.newInstance(this);
        if (Objects.nonNull(imgHandle) && Objects.nonNull(stream)) {
            imgHandle.createFromStream(stream);
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

    public BufferedImage convertToBufferedImage(TextureEntity textureEntity) {
        return imgHandle.convertToBufferedImage(textureEntity);
    }

    public byte[] convertToByte(TextureEntity textureEntity) {
        return imgHandle.convertToByte(textureEntity);
    }

    public String getName() {
        return StringUtils.substringAfterLast(path, ".");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ImgEntity imgEntity = (ImgEntity) o;
        return Objects.equals(path, imgEntity.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
