package xyz.hooy.npk.codec;

import xyz.hooy.npk.Texture;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

public interface ImgDecoder {

    Texture decode(ImageInputStream stream) throws IOException;

    int version();
}
