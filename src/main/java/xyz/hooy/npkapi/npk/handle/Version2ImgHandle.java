package xyz.hooy.npkapi.npk.handle;

import xyz.hooy.npkapi.component.MemoryStream;
import xyz.hooy.npkapi.npk.constant.AlbumModes;
import xyz.hooy.npkapi.npk.constant.ColorLinkModes;
import xyz.hooy.npkapi.npk.constant.CompressModes;
import xyz.hooy.npkapi.npk.entity.Album;
import xyz.hooy.npkapi.npk.entity.Sprite;
import xyz.hooy.npkapi.npk.util.BufferedImageUtils;
import xyz.hooy.npkapi.npk.util.CompressUtils;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Version2ImgHandle extends AbstractHandle {

    public Version2ImgHandle(Album album) {
        super(album);
    }

    @Override
    public void createFromStream(MemoryStream stream) {
        Map<Sprite, Integer> map = new HashMap<>();
        long pos = stream.position() + album.getIndexLength();
        for (int i = 0; i < album.getCount(); i++) {
            Sprite sprite = new Sprite(album);
            sprite.setIndex(album.getSprites().size());
            sprite.setColorLink(ColorLinkModes.valueOf(stream.readInt()));
            album.getSprites().add(sprite);
            if (sprite.getColorLink() == ColorLinkModes.LINK) {
                map.put(sprite, stream.readInt());
                continue;
            }
            sprite.setCompress(CompressModes.valueOf(stream.readInt()));
            sprite.setWidth(stream.readInt());
            sprite.setHeight(stream.readInt());
            sprite.setLength(stream.readInt());
            sprite.setX(stream.readInt());
            sprite.setY(stream.readInt());
            sprite.setFrameWidth(stream.readInt());
            sprite.setFrameHeight(stream.readInt());
        }
        if (stream.position() < pos) {
            album.getSprites().clear();
            return;
        }
        for (Sprite sprite : album.getSprites()) {
            if (sprite.getColorLink() == ColorLinkModes.LINK) {
                if (map.containsKey(sprite) && map.get(sprite) < album.getSprites().size() && map.get(sprite) > -1 && map.get(sprite).equals(sprite.getIndex())) {
                    sprite.setTarget(album.getSprites().get(map.get(sprite)));
                    sprite.setWidth(sprite.getTarget().getWidth());
                    sprite.setHeight(sprite.getTarget().getHeight());
                    sprite.setFrameWidth(sprite.getTarget().getFrameWidth());
                    sprite.setFrameHeight(sprite.getTarget().getFrameHeight());
                    sprite.setX(sprite.getTarget().getX());
                    sprite.setY(sprite.getTarget().getY());
                } else {
                    album.getSprites().clear();
                    return;
                }
                continue;
            }
            if (sprite.getCompress() == CompressModes.NONE) {
                sprite.setLength(sprite.getWidth() * sprite.getHeight() * (sprite.getColorLink() == ColorLinkModes.ARGB_8888 ? 4 : 2));
            }
            byte[] data = new byte[sprite.getLength()];
            stream.read(data);
            sprite.setData(data);
        }
    }

    @Override
    public BufferedImage convertToBufferedImage(Sprite sprite) {
        byte[] data = sprite.getData();
        if (sprite.getCompress() == CompressModes.ZLIB) {
            data = CompressUtils.zlibDecompress(data);
        }
        return BufferedImageUtils.fromArray(data, sprite.getWidth(), sprite.getHeight(), sprite.getColorLink());
    }

    @Override
    public byte[] convertToByte(Sprite sprite) {
        // TODO: 可能存在 Bug
        if (sprite.getColorLink().getValue() > ColorLinkModes.LINK.getValue()) {
            int value = sprite.getColorLink().getValue() - 4;
            sprite.setColorLink(ColorLinkModes.valueOf(value));
        }
        if (sprite.getCompress().getValue() > CompressModes.ZLIB.getValue()) {
            sprite.setCompress(CompressModes.ZLIB);
        }
        return BufferedImageUtils.toArray(sprite.getPicture(), sprite.getColorLink());
    }

    @Override
    public void newImage(int count, ColorLinkModes type, int index) {
        if (count < 1) {
            return;
        }
        Sprite[] array = new Sprite[count];
        array[0] = new Sprite(album);
        array[0].setIndex(index);
        if (type != ColorLinkModes.LINK) {
            array[0].setColorLink(type);
        }
        for (int i = 1; i < count; i++) {
            array[i] = new Sprite(album);
            array[i].setColorLink(type);
            if (type == ColorLinkModes.LINK) {
                array[i].setTarget(array[0]);
            }
            array[i].setIndex(index + i);
        }
        for (int i = 0; i < array.length; i++) {
            album.getSprites().add(index + i, array[i]);
        }
    }

    @Override
    public byte[] adjustData() {
        MemoryStream stream = new MemoryStream();
        for (Sprite sprite : album.getSprites()) {
            stream.writeInt(sprite.getColorLink().getValue());
            if (sprite.getColorLink() == ColorLinkModes.LINK && Objects.nonNull(sprite.getTarget())) {
                stream.writeInt(sprite.getTarget().getIndex());
                continue;
            }
            stream.writeInt(sprite.getCompress().getValue());
            stream.writeInt(sprite.getWidth());
            stream.writeInt(sprite.getHeight());
            stream.writeInt(sprite.getLength());
            stream.writeInt(sprite.getX());
            stream.writeInt(sprite.getY());
            stream.writeInt(sprite.getFrameWidth());
            stream.writeInt(sprite.getFrameHeight());
        }
        album.setIndexLength(stream.length());
        for (Sprite sprite : album.getSprites()) {
            if (sprite.getColorLink() == ColorLinkModes.LINK) {
                continue;
            }
            stream.write(sprite.getData());
        }
        return stream.toArray();
    }

    @Override
    public void convertToVersion(AlbumModes version) {
        if (version == AlbumModes.VERSION_4 || version == AlbumModes.VERSION_6) {
            album.getSprites().forEach(item -> item.setColorLink(ColorLinkModes.ARGB_1555));
        }
    }
}
