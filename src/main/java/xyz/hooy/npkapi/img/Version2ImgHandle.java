package xyz.hooy.npkapi.img;

import xyz.hooy.npkapi.component.MemoryStream;
import xyz.hooy.npkapi.constant.AlbumModes;
import xyz.hooy.npkapi.constant.ColorLinkModes;
import xyz.hooy.npkapi.constant.CompressModes;
import xyz.hooy.npkapi.entity.Album;
import xyz.hooy.npkapi.entity.Sprite;
import xyz.hooy.npkapi.util.BufferedImageUtils;
import xyz.hooy.npkapi.util.CompressUtils;

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
            Sprite texture = new Sprite(album);
            texture.setIndex(album.getSprites().size());
            texture.setColorLink(ColorLinkModes.valueOf(stream.readInt()));
            album.getSprites().add(texture);
            if (texture.getColorLink() == ColorLinkModes.LINK) {
                map.put(texture, stream.readInt());
                continue;
            }
            texture.setCompress(CompressModes.valueOf(stream.readInt()));
            texture.setWidth(stream.readInt());
            texture.setHeight(stream.readInt());
            texture.setLength(stream.readInt());
            texture.setX(stream.readInt());
            texture.setY(stream.readInt());
            texture.setFrameWidth(stream.readInt());
            texture.setFrameHeight(stream.readInt());
        }
        if (stream.position() < pos) {
            album.getSprites().clear();
            return;
        }
        for (Sprite texture : album.getSprites()) {
            if (texture.getColorLink() == ColorLinkModes.LINK) {
                if (map.containsKey(texture) && map.get(texture) < album.getSprites().size() && map.get(texture) > -1 && map.get(texture).equals(texture.getIndex())) {
                    texture.setTarget(album.getSprites().get(map.get(texture)));
                    texture.setWidth(texture.getTarget().getWidth());
                    texture.setHeight(texture.getTarget().getHeight());
                    texture.setFrameWidth(texture.getTarget().getFrameWidth());
                    texture.setFrameHeight(texture.getTarget().getFrameHeight());
                    texture.setX(texture.getTarget().getX());
                    texture.setY(texture.getTarget().getY());
                } else {
                    album.getSprites().clear();
                    return;
                }
                continue;
            }
            if (texture.getCompress() == CompressModes.NONE) {
                texture.setLength(texture.getWidth() * texture.getHeight() * (texture.getColorLink() == ColorLinkModes.ARGB_8888 ? 4 : 2));
            }
            byte[] data = new byte[texture.getLength()];
            stream.read(data);
            texture.setData(data);
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
        album.setIndexLength((long) stream.length());
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
