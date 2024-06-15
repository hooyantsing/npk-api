package xyz.hooy.npkapi.npk.handle;

import xyz.hooy.npkapi.component.MemoryStream;
import xyz.hooy.npkapi.npk.entity.Album;
import xyz.hooy.npkapi.npk.entity.Sprite;

import java.awt.image.BufferedImage;

public class OggHandle extends AbstractHandle {

    private byte[] data = new byte[0];

    public OggHandle(Album album) {
        super(album);
    }

    @Override
    public void createFromStream(MemoryStream stream) {
        data = stream.read(album.getLength());
    }

    @Override
    public BufferedImage convertToBufferedImage(Sprite sprite) {
        return null;
    }

    @Override
    public byte[] convertToByte(Sprite sprite) {
        return new byte[0];
    }

    @Override
    public byte[] adjustData() {
        return data;
    }
}
