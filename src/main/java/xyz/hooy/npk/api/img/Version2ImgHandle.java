package xyz.hooy.npk.api.img;

import xyz.hooy.npk.api.constant.ColorLinkTypes;
import xyz.hooy.npk.api.constant.CompressModes;
import xyz.hooy.npk.api.constant.ImgVersions;
import xyz.hooy.npk.api.entity.ImgEntity;
import xyz.hooy.npk.api.entity.TextureEntity;
import xyz.hooy.npk.api.util.BufferedImageUtils;
import xyz.hooy.npk.api.util.CompressUtils;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Version2ImgHandle extends AbstractImgHandle {

    public Version2ImgHandle(ImgEntity imgEntity) {
        super(imgEntity);
    }

    @Override
    public void createFromBuffer(ByteBuffer buffer) {
        Map<TextureEntity, Integer> map = new HashMap<>();
        long pos = buffer.position() + imgEntity.getIndexLength();
        for (int i = 0; i < imgEntity.getCount(); i++) {
            TextureEntity texture = new TextureEntity(imgEntity);
            texture.setIndex(imgEntity.getTextureEntities().size());
            texture.setType(ColorLinkTypes.valueOf(buffer.getInt()));
            imgEntity.getTextureEntities().add(texture);
            if (texture.getType() == ColorLinkTypes.LINK) {
                map.put(texture, buffer.getInt());
                continue;
            }
            texture.setCompress(CompressModes.valueOf(buffer.getInt()));
            texture.setWidth(buffer.getInt());
            texture.setHeight(buffer.getInt());
            texture.setLength(buffer.getInt());
            texture.setX(buffer.getInt());
            texture.setY(buffer.getInt());
            texture.setFrameWidth(buffer.getInt());
            texture.setFrameHeight(buffer.getInt());
        }
        if (buffer.position() < pos) {
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
            buffer.get(data);
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
        ByteBuffer buffer = ByteBuffer.allocate(128);
        for (TextureEntity textureEntity : imgEntity.getTextureEntities()) {
            buffer.putInt(textureEntity.getType().getValue());
            if (textureEntity.getType() == ColorLinkTypes.LINK && Objects.nonNull(textureEntity.getTarget())) {
                buffer.putInt(textureEntity.getTarget().getIndex());
                continue;
            }
            buffer.putInt(textureEntity.getCompress().getValue());
            buffer.putInt(textureEntity.getWidth());
            buffer.putInt(textureEntity.getHeight());
            buffer.putInt(textureEntity.getLength());
            buffer.putInt(textureEntity.getX());
            buffer.putInt(textureEntity.getY());
            buffer.putInt(textureEntity.getFrameWidth());
            buffer.putInt(textureEntity.getFrameHeight());
        }
        imgEntity.setIndexLength((long) buffer.array().length);
        for (TextureEntity textureEntity : imgEntity.getTextureEntities()) {
            if (textureEntity.getType() == ColorLinkTypes.LINK) {
                continue;
            }
            buffer.put(textureEntity.getTextureData());
        }
        return buffer.array();
    }

    @Override
    public void convertToVersion(ImgVersions version) {
        if (version == ImgVersions.VERSION_4 || version == ImgVersions.VERSION_6) {
            imgEntity.getTextureEntities().forEach(item -> item.setType(ColorLinkTypes.ARGB_1555));
        }
    }
}
