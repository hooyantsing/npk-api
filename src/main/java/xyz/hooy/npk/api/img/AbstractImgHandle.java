package xyz.hooy.npk.api.img;

import lombok.Getter;
import lombok.SneakyThrows;
import xyz.hooy.npk.api.coder.NpkCoder;
import xyz.hooy.npk.api.constant.ColorLinkTypes;
import xyz.hooy.npk.api.constant.ImgVersions;
import xyz.hooy.npk.api.entity.ImgEntity;
import xyz.hooy.npk.api.entity.TextureEntity;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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

    public void adjust() {
        for (TextureEntity textureEntity : imgEntity.getTextureEntities()) {
            textureEntity.adjust();
        }
        imgEntity.setCount(imgEntity.getTextureEntities().size());
        // TODO: 指定合适的大小
        ByteBuffer buffer = ByteBuffer.allocate(128);
        byte[] data = adjustData();
        if (imgEntity.getImgVersion().getValue() > ImgVersions.VERSION_1.getValue()) {
            buffer.put(NpkCoder.IMG_FLAG.getBytes(StandardCharsets.UTF_8));
            buffer.putLong(imgEntity.getIndexLength());
            buffer.putInt(imgEntity.getImgVersion().getValue());
            buffer.putInt(imgEntity.getCount());
        }
        buffer.put(data);
        imgEntity.setImgData(buffer.array());
        imgEntity.setLength(imgEntity.getImgData().length);
    }

    public byte[] adjustData() {
        return new byte[0];
    }

    public void convertToVersion(ImgVersions version) {
    }
}
