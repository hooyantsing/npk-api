package xyz.hooy.npk.api.img;

import xyz.hooy.npk.api.entity.ImgEntity;

import java.nio.ByteBuffer;

public class Version4ImgHandle extends AbstractImgHandle {

    public Version4ImgHandle(ImgEntity imgEntity) {
        super(imgEntity);
    }

    @Override
    public void createFromBuffer(ByteBuffer buffer) {
        throw new UnsupportedOperationException();
    }
}
