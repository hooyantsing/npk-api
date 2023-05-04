package xyz.hooy.npk.api.util;

import xyz.hooy.npk.api.color.ColorFactory;
import xyz.hooy.npk.api.model.Texture;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public final class TextureUtils {

    public static void toPng(Texture texture, String fileName) throws IOException {
        ImageIO.write(ColorFactory.process(texture), "png", new File(fileName));
    }
}
