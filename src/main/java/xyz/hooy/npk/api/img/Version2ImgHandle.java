package xyz.hooy.npk.api.img;

import xyz.hooy.npk.api.constant.ColorLinkTypes;
import xyz.hooy.npk.api.constant.CompressModes;
import xyz.hooy.npk.api.entity.ImgEntity;
import xyz.hooy.npk.api.entity.TextureEntity;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class Version2ImgHandle extends AbstractImgHandle {

    public Version2ImgHandle(ImgEntity imgEntity) {
        super(imgEntity);
    }

    @Override
    public void createFromBuffer(ByteBuffer buffer) {
        Map<TextureEntity, Integer> map = new HashMap<>();
        int pos = buffer.position() + imgEntity.getIndexLength();
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
                texture.setLength(texture.getWidth() * texture.getHeight() * (texture.getType() == ColorLinkTypes.ARGB8888 ? 4 : 2));
            }
            byte[] data = new byte[texture.getLength()];
            buffer.get(data);
            texture.setTextureData(data);
        }
    }
}
