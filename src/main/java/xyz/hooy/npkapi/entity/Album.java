package xyz.hooy.npkapi.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.hooy.npkapi.component.MemoryStream;
import xyz.hooy.npkapi.constant.AlbumModes;
import xyz.hooy.npkapi.constant.AlbumSuffixModes;
import xyz.hooy.npkapi.constant.CompressModes;
import xyz.hooy.npkapi.img.AbstractHandle;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class Album {

    private Integer length;
    private AlbumModes albumModes = AlbumModes.VERSION_2;
    private Integer count;
    private byte[] data;

    private String path;
    private Integer offset;
    private Long indexLength;
    private AbstractHandle handle;
    private List<Sprite> sprites = new ArrayList<>();
    private Album target;

    public Album() {
        this.handle = AbstractHandle.newInstance(this);
    }

    public Album(List<BufferedImage> bufferedImages) {
        this();
        for (int i = 0; i < bufferedImages.size(); i++) {
            BufferedImage bufferedImage = bufferedImages.get(i);
            Sprite sprite = new Sprite(this);
            sprite.setIndex(i);
            sprite.setPicture(bufferedImage);
            sprite.setCompress(CompressModes.ZLIB);
            sprite.setX(bufferedImage.getWidth());
            sprite.setY(bufferedImage.getHeight());
            sprite.setFrameWidth(bufferedImage.getWidth());
            sprite.setFrameHeight(bufferedImage.getHeight());
            sprites.add(sprite);
        }
        adjust();
    }

    public Album(BufferedImage[] array) {
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
        for (int i = 0; i < sprites.size(); i++) {
            sprites.get(i).setIndex(i);
            sprites.get(i).setParent(this);
        }
    }

    public BufferedImage convertToBufferedImage(Sprite sprite) {
        return handle.convertToBufferedImage(sprite);
    }

    public byte[] convertToByte(Sprite sprite) {
        return handle.convertToByte(sprite);
    }

    public String getName() {
        return path.substring(path.indexOf("." + 1));
    }

    public AlbumSuffixModes getAlbumSuffixMode() {
        return getName().endsWith("img") ? AlbumSuffixModes.IMAGE : AlbumSuffixModes.AUDIO;
    }

    public void addSprite(Sprite sprite) {
        sprites.add(sprite);
        adjust();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Album album = (Album) o;
        return Objects.equals(path, album.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
