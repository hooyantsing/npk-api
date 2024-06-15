package xyz.hooy.npkapi.npk.handle;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.component.MemoryStream;
import xyz.hooy.npkapi.npk.constant.AlbumModes;
import xyz.hooy.npkapi.npk.constant.ColorLinkModes;
import xyz.hooy.npkapi.npk.entity.Album;
import xyz.hooy.npkapi.npk.entity.Sprite;
import xyz.hooy.npkapi.npk.NpkCore;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Getter
public abstract class AbstractHandle {

    private static final Map<AlbumModes, Class<? extends AbstractHandle>> versionMap = new HashMap<>();

    static {
        register(AlbumModes.VERSION_2, Version2ImgHandle.class);
        register(AlbumModes.VERSION_4, Version4ImgHandle.class);
        register(AlbumModes.OGG, OggHandle.class);
    }

    private static void register(AlbumModes version, Class<? extends AbstractHandle> handle) {
        versionMap.put(version, handle);
        log.info("Support npk version: {}.", version.name());
    }

    @SneakyThrows
    public static AbstractHandle newInstance(Album album) {
        Class<? extends AbstractHandle> abstractImgHandle = versionMap.get(album.getAlbumModes());
        if (Objects.isNull(abstractImgHandle)) {
            throw new UnsupportedOperationException(String.format("The current handle is not supported %s.", album.getAlbumModes()));
        }
        return abstractImgHandle.getConstructor(Album.class).newInstance(album);
    }

    protected Album album;

    public AbstractHandle(Album album) {
        this.album = album;
    }

    public abstract void createFromStream(MemoryStream stream);

    public abstract BufferedImage convertToBufferedImage(Sprite sprite);

    public abstract byte[] convertToByte(Sprite sprite);

    public void newImage(int count, ColorLinkModes type, int index) {
    }

    public void adjust() {
        for (Sprite sprite : album.getSprites()) {
            sprite.adjust();
        }
        album.setCount(album.getSprites().size());
        MemoryStream stream = new MemoryStream();
        byte[] data = adjustData();
        if (album.getAlbumModes().getValue() > AlbumModes.VERSION_1.getValue()) {
            stream.write(NpkCore.IMG_FLAG.getBytes(StandardCharsets.UTF_8));
            stream.writeLong(album.getIndexLength());
            stream.writeInt(album.getAlbumModes().getValue());
            stream.writeInt(album.getCount());
        }
        stream.write(data);
        album.setData(stream.toArray());
        album.setLength(album.getData().length);
    }

    public byte[] adjustData() {
        return new byte[0];
    }

    public void convertToVersion(AlbumModes version) {
    }
}
