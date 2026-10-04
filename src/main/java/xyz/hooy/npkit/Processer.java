package xyz.hooy.npkit;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

public interface Processer {

    void read(ImageOutputStream stream) throws IOException;

    void write(ImageOutputStream stream) throws IOException;
}
