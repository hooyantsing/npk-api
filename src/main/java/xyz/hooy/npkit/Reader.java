package xyz.hooy.npkit;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

public interface Reader {

    void read(ImageInputStream stream) throws IOException;
}
