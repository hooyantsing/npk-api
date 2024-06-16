package xyz.hooy.npkapi.npk.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.hooy.npkapi.component.BufferedAudio;
import xyz.hooy.npkapi.component.MemoryStream;
import xyz.hooy.npkapi.npk.constant.AlbumModes;
import xyz.hooy.npkapi.npk.constant.AlbumSuffixModes;
import xyz.hooy.npkapi.npk.constant.CompressModes;
import xyz.hooy.npkapi.npk.handle.AbstractHandle;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class Album {

    private Integer length;
    private AlbumModes albumModes;
    private Integer count;
    private byte[] data;

    private String path;
    private Integer offset;
    private Integer indexLength;
    private AbstractHandle handle;
    private List<Sprite> sprites = new ArrayList<>();
    private Album target;

    public Album() {
        this.albumModes = AlbumModes.VERSION_2;
        this.handle = AbstractHandle.newInstance(this);
    }

    public Album(BufferedImage bufferedImage) {
        this();
        Sprite sprite = bufferedImageToSprite(bufferedImage, 0);
        this.sprites.add(sprite);
        adjust();
    }

    public Album(List<BufferedImage> bufferedImages) {
        this();
        for (int i = 0; i < bufferedImages.size(); i++) {
            BufferedImage bufferedImage = bufferedImages.get(i);
            Sprite sprite = bufferedImageToSprite(bufferedImage, i);
            this.sprites.add(sprite);
        }
        adjust();
    }

    public Album(BufferedAudio bufferedAudio) {
        this.albumModes = AlbumModes.OGG;
        this.length = bufferedAudio.getLength();
        this.indexLength = 0;
        MemoryStream memoryStream = new MemoryStream(bufferedAudio.getLength());
        memoryStream.write(bufferedAudio.getData());
        initHandle(memoryStream);
        adjust();
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

    public AlbumSuffixModes getAlbumSuffixMode() {
        return path.endsWith("ogg") ? AlbumSuffixModes.AUDIO : AlbumSuffixModes.IMAGE;
    }

    public void addSprite(Sprite sprite) {
        sprites.add(sprite);
        adjust();
    }

    private Sprite bufferedImageToSprite(BufferedImage bufferedImage, int index) {
        Sprite sprite = new Sprite(this);
        sprite.setIndex(index);
        sprite.setPicture(bufferedImage);
        sprite.setCompress(CompressModes.ZLIB);
        sprite.setX(bufferedImage.getWidth());
        sprite.setY(bufferedImage.getHeight());
        sprite.setFrameWidth(bufferedImage.getWidth());
        sprite.setFrameHeight(bufferedImage.getHeight());
        return sprite;
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
