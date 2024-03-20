package xyz.hooy.npkapi.img;

import xyz.hooy.npkapi.component.MemoryStream;
import xyz.hooy.npkapi.entity.ImgEntity;
import xyz.hooy.npkapi.entity.TextureEntity;

import java.awt.image.BufferedImage;

public class OggHandle extends AbstractHandle {

    private byte[] data = new byte[0];

    public OggHandle(ImgEntity imgEntity) {
        super(imgEntity);
    }

    @Override
    public void createFromStream(MemoryStream stream) {
        int length = Math.toIntExact(imgEntity.getIndexLength());
        data = stream.read(length);
    }

    @Override
    public BufferedImage convertToBufferedImage(TextureEntity textureEntity) {
        return null;
    }

    @Override
    public byte[] convertToByte(TextureEntity textureEntity) {
        return new byte[0];
    }

    @Override
    public byte[] adjustData() {
        return data;
    }
}
