package xyz.hooy.npk.api.img;

import lombok.Getter;
import lombok.SneakyThrows;
import xyz.hooy.npk.api.constant.ColorLinkTypes;
import xyz.hooy.npk.api.constant.ImgVersions;
import xyz.hooy.npk.api.entity.ImgEntity;
import xyz.hooy.npk.api.entity.TextureEntity;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
public abstract class AbstractImgHandle {

    private static final Map<ImgVersions, Class<? extends AbstractImgHandle>> versionMap = new HashMap<>();

    static {
        register(ImgVersions.VERSION_2, Version2ImgHandle.class);
        register(ImgVersions.VERSION_4, Version4ImgHandle.class);
    }

    private static void register(ImgVersions version, Class<? extends AbstractImgHandle> handle) {
        versionMap.put(version, handle);
    }

    @SneakyThrows
    public static AbstractImgHandle newInstance(ImgEntity imgEntity) {
        Class<? extends AbstractImgHandle> abstractImgHandle = versionMap.get(imgEntity.getImgVersion());
        if (Objects.isNull(abstractImgHandle)) {
            throw new UnsupportedOperationException(String.format("The current handle is not supported %s", imgEntity.getImgVersion()));
        }
        return abstractImgHandle.getConstructor(ImgEntity.class).newInstance(imgEntity);
    }

    protected ImgEntity imgEntity;

    public AbstractImgHandle(ImgEntity imgEntity) {
        this.imgEntity = imgEntity;
    }

    public abstract void createFromBuffer(ByteBuffer buffer);

    public abstract BufferedImage convertToBufferedImage(TextureEntity textureEntity);

    public abstract byte[] convertToByte(TextureEntity textureEntity);

    public void newImage(int count, ColorLinkTypes type, int index) {
    }
}
