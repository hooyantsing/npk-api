package xyz.hooy.npkapi.img;

import xyz.hooy.npkapi.component.MemoryStream;
import xyz.hooy.npkapi.entity.Album;
import xyz.hooy.npkapi.entity.Sprite;

import java.awt.image.BufferedImage;

public class OggHandle extends AbstractHandle {

    private byte[] data = new byte[0];

    public OggHandle(Album album) {
        super(album);
    }

    @Override
    public void createFromStream(MemoryStream stream) {
        int length = Math.toIntExact(album.getIndexLength());
        data = stream.read(length);
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
