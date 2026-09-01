package npkapi;

import xyz.hooy.npkit.Npkit;
import xyz.hooy.npkit.Version2ImgWriter;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class NpkitTest {

    public void test() throws IOException {
        try (ImageOutputStream outputStream = new FileImageOutputStream(new File(""))) {
            Version2ImgWriter version2ImgWriter = new Version2ImgWriter();
            version2ImgWriter.encodeFrame(new BufferedImage(0, 0, 0));
            Npkit.imgWriter().add("/img/test/1", version2ImgWriter).add("/img/test/2", version2ImgWriter).write(outputStream);
        }
    }
}
