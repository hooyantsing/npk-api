package xyz.hooy.npkit;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

public interface Writer {

    void write(ImageOutputStream stream) throws IOException;
}
