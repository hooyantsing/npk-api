package xyz.hooy.npkapi.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.hooy.npkapi.component.MemoryStream;
import xyz.hooy.npkapi.constant.CompressModes;
import xyz.hooy.npkapi.constant.ImgVersions;
import xyz.hooy.npkapi.img.AbstractHandle;

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
    private AbstractHandle handle;
    private List<TextureEntity> textureEntities = new ArrayList<>();
    private ImgEntity target;

    public ImgEntity() {
        this.handle = AbstractHandle.newInstance(this);
    }

    public ImgEntity(List<BufferedImage> bufferedImages) {
        this();
        for (int i = 0; i < bufferedImages.size(); i++) {
            BufferedImage bufferedImage = bufferedImages.get(i);
            TextureEntity textureEntity = new TextureEntity(this);
            textureEntity.setIndex(i);
            textureEntity.setPicture(bufferedImage);
            textureEntity.setCompress(CompressModes.ZLIB);
            textureEntity.setX(bufferedImage.getWidth());
            textureEntity.setY(bufferedImage.getHeight());
            textureEntity.setFrameWidth(bufferedImage.getWidth());
            textureEntity.setFrameHeight(bufferedImage.getHeight());
            textureEntities.add(textureEntity);
        }
        adjust();
    }

    public ImgEntity(BufferedImage[] array) {
        this(Arrays.asList(array));
    }

    public void initHandle(MemoryStream stream) {
        handle = AbstractHandle.newInstance(this);
        if (Objects.nonNull(stream)) {
            handle.createFromStream(stream);
        }
    }

    public void adjust() {
        if (Objects.nonNull(target)) {
            return;
        }
        adjustIndex();
        handle.adjust();
    }

    public void adjustIndex() {
        for (int i = 0; i < textureEntities.size(); i++) {
            textureEntities.get(i).setIndex(i);
            textureEntities.get(i).setParent(this);
        }
    }

    public BufferedImage convertToBufferedImage(TextureEntity textureEntity) {
        return handle.convertToBufferedImage(textureEntity);
    }

    public byte[] convertToByte(TextureEntity textureEntity) {
        return handle.convertToByte(textureEntity);
    }

    public String getName() {
        return path.substring(path.indexOf("." + 1));
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
