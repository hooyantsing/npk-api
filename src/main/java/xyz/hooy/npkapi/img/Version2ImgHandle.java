package xyz.hooy.npkapi.img;

import xyz.hooy.npkapi.component.MemoryStream;
import xyz.hooy.npkapi.constant.ColorLinkTypes;
import xyz.hooy.npkapi.constant.CompressModes;
import xyz.hooy.npkapi.constant.ImgVersions;
import xyz.hooy.npkapi.entity.ImgEntity;
import xyz.hooy.npkapi.entity.TextureEntity;
import xyz.hooy.npkapi.util.BufferedImageUtils;
import xyz.hooy.npkapi.util.CompressUtils;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Version2ImgHandle extends AbstractImgHandle {

    public Version2ImgHandle(ImgEntity imgEntity) {
        super(imgEntity);
    }

    @Override
    public void createFromStream(MemoryStream stream) {
        Map<TextureEntity, Integer> map = new HashMap<>();
        long pos = stream.position() + imgEntity.getIndexLength();
        for (int i = 0; i < imgEntity.getCount(); i++) {
            TextureEntity texture = new TextureEntity(imgEntity);
            texture.setIndex(imgEntity.getTextureEntities().size());
            texture.setType(ColorLinkTypes.valueOf(stream.readInt()));
            imgEntity.getTextureEntities().add(texture);
            if (texture.getType() == ColorLinkTypes.LINK) {
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
            imgEntity.getTextureEntities().clear();
            return;
        }
        for (TextureEntity texture : imgEntity.getTextureEntities()) {
            if (texture.getType() == ColorLinkTypes.LINK) {
                if (map.containsKey(texture) && map.get(texture) < imgEntity.getTextureEntities().size() && map.get(texture) > -1 && map.get(texture).equals(texture.getIndex())) {
                    texture.setTarget(imgEntity.getTextureEntities().get(map.get(texture)));
                    texture.setWidth(texture.getTarget().getWidth());
                    texture.setHeight(texture.getTarget().getHeight());
                    texture.setFrameWidth(texture.getTarget().getFrameWidth());
                    texture.setFrameHeight(texture.getTarget().getFrameHeight());
                    texture.setX(texture.getTarget().getX());
                    texture.setY(texture.getTarget().getY());
                } else {
                    imgEntity.getTextureEntities().clear();
                    return;
                }
                continue;
            }
            if (texture.getCompress() == CompressModes.NONE) {
                texture.setLength(texture.getWidth() * texture.getHeight() * (texture.getType() == ColorLinkTypes.ARGB_8888 ? 4 : 2));
            }
            byte[] data = new byte[texture.getLength()];
            stream.read(data);
            texture.setTextureData(data);
        }
    }

    @Override
    public BufferedImage convertToBufferedImage(TextureEntity textureEntity) {
        byte[] textureData = textureEntity.getTextureData();
        if (textureEntity.getCompress() == CompressModes.ZLIB) {
            textureData = CompressUtils.zlibDecompress(textureData);
        }
        return BufferedImageUtils.fromArray(textureData, textureEntity.getWidth(), textureEntity.getHeight(), textureEntity.getType());
    }

    @Override
    public byte[] convertToByte(TextureEntity textureEntity) {
        // TODO: 可能存在 Bug
        if (textureEntity.getType().getValue() > ColorLinkTypes.LINK.getValue()) {
            int value = textureEntity.getType().getValue() - 4;
            textureEntity.setType(ColorLinkTypes.valueOf(value));
        }
        if (textureEntity.getCompress().getValue() > CompressModes.ZLIB.getValue()) {
            textureEntity.setCompress(CompressModes.ZLIB);
        }
        return BufferedImageUtils.toArray(textureEntity.getPicture(), textureEntity.getType());
    }

    @Override
    public void newImage(int count, ColorLinkTypes type, int index) {
        if (count < 1) {
            return;
        }
        TextureEntity[] array = new TextureEntity[count];
        array[0] = new TextureEntity(imgEntity);
        array[0].setIndex(index);
        if (type != ColorLinkTypes.LINK) {
            array[0].setType(type);
        }
        for (int i = 1; i < count; i++) {
            array[i] = new TextureEntity(imgEntity);
            array[i].setType(type);
            if (type == ColorLinkTypes.LINK) {
                array[i].setTarget(array[0]);
            }
            array[i].setIndex(index + i);
        }
        for (int i = 0; i < array.length; i++) {
            imgEntity.getTextureEntities().add(index + i, array[i]);
        }
    }

    @Override
    public byte[] adjustData() {
        // TODO: 指定合适的大小
        MemoryStream stream = new MemoryStream(128);
        for (TextureEntity textureEntity : imgEntity.getTextureEntities()) {
            stream.writeInt(textureEntity.getType().getValue());
            if (textureEntity.getType() == ColorLinkTypes.LINK && Objects.nonNull(textureEntity.getTarget())) {
                stream.writeInt(textureEntity.getTarget().getIndex());
                continue;
            }
            stream.writeInt(textureEntity.getCompress().getValue());
            stream.writeInt(textureEntity.getWidth());
            stream.writeInt(textureEntity.getHeight());
            stream.writeInt(textureEntity.getLength());
            stream.writeInt(textureEntity.getX());
            stream.writeInt(textureEntity.getY());
            stream.writeInt(textureEntity.getFrameWidth());
            stream.writeInt(textureEntity.getFrameHeight());
        }
        imgEntity.setIndexLength((long) stream.length());
        for (TextureEntity textureEntity : imgEntity.getTextureEntities()) {
            if (textureEntity.getType() == ColorLinkTypes.LINK) {
                continue;
            }
            stream.write(textureEntity.getTextureData());
        }
        return stream.toArray();
    }

    @Override
    public void convertToVersion(ImgVersions version) {
        if (version == ImgVersions.VERSION_4 || version == ImgVersions.VERSION_6) {
            imgEntity.getTextureEntities().forEach(item -> item.setType(ColorLinkTypes.ARGB_1555));
        }
    }
}
