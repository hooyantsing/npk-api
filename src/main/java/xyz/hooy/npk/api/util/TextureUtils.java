package xyz.hooy.npk.api.util;

import xyz.hooy.npk.api.color.ColorFactory;
import xyz.hooy.npk.api.entity.TextureEntity;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
public final class TextureUtils {

    public static void toPng(TextureEntity texture, String fileName) throws IOException {
        ImageIO.write(ColorFactory.process(texture), "png", new File(fileName));
    }
}
