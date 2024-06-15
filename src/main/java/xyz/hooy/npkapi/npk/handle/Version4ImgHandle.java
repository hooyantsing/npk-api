package xyz.hooy.npkapi.npk.handle;

import xyz.hooy.npkapi.component.MemoryStream;
import xyz.hooy.npkapi.npk.entity.Album;
import xyz.hooy.npkapi.npk.entity.Sprite;

import java.awt.image.BufferedImage;

public class Version4ImgHandle extends AbstractHandle {

    public Version4ImgHandle(Album album) {
        super(album);
    }

    @Override
    public void createFromStream(MemoryStream stream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BufferedImage convertToBufferedImage(Sprite sprite) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] convertToByte(Sprite sprite) {
        throw new UnsupportedOperationException();
    }
}
