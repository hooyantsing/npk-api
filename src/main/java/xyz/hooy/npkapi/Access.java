package xyz.hooy.npkapi;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

public interface Access {

    void read(ImageInputStream stream) throws IOException;

    void write(ImageOutputStream stream) throws IOException;
}
