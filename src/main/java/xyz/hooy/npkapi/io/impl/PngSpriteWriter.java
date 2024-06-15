package xyz.hooy.npkapi.io.impl;

import xyz.hooy.npkapi.io.SpriteWriter;
import xyz.hooy.npkapi.npk.entity.Sprite;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PngSpriteWriter implements SpriteWriter {
    @Override
    public void write(String path, Sprite sprite) throws IOException {
        BufferedImage bufferedImage = sprite.getPicture();
        ImageIO.write(bufferedImage, suffix(), new File(path));
    }

    @Override
    public String suffix() {
        return "png";
    }
}
