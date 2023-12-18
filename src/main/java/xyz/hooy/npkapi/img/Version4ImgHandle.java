package xyz.hooy.npkapi.img;

import xyz.hooy.npkapi.component.MemoryStream;
import xyz.hooy.npkapi.entity.ImgEntity;
import xyz.hooy.npkapi.entity.TextureEntity;

import java.awt.image.BufferedImage;

public class Version4ImgHandle extends AbstractImgHandle {

    public Version4ImgHandle(ImgEntity imgEntity) {
        super(imgEntity);
    }

    @Override
    public void createFromStream(MemoryStream stream) {
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
