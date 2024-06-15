package xyz.hooy.npkapi.io;

public class JpegSpriteReader extends PngSpriteReader{

    public JpegSpriteReader(String path) {
        super(path);
    }
    @Override
    public String suffix() {
        return "jpg";
    }
}
