package xyz.hooy.npk.api.img;

import xyz.hooy.npk.api.entity.ImgEntity;
import xyz.hooy.npk.api.entity.TextureEntity;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class Version4ImgHandle extends AbstractImgHandle {

    public Version4ImgHandle(ImgEntity imgEntity) {
        super(imgEntity);
    }

    @Override
    public void createFromBuffer(ByteBuffer buffer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BufferedImage convertToBufferedImage(TextureEntity textureEntity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] convertToByte(TextureEntity textureEntity) {
        throw new UnsupportedOperationException();
    }
}
