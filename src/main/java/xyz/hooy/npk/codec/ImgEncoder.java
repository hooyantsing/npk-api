package xyz.hooy.npk.codec;

import xyz.hooy.npk.Texture;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

public interface ImgEncoder {

    void encode(ImageOutputStream stream, Texture texture) throws IOException;

    int version();
}
