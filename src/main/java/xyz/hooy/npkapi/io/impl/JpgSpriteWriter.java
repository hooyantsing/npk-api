package xyz.hooy.npkapi.io.impl;

import xyz.hooy.npkapi.io.SpriteWriter;
import xyz.hooy.npkapi.npk.entity.Sprite;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class JpgSpriteWriter implements SpriteWriter {
    @Override
    public void write(String path, Sprite sprite) throws IOException {
        BufferedImage bufferedImage = sprite.getPicture();
        BufferedImage imageWithoutAlpha = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imageWithoutAlpha.createGraphics();
        g.drawImage(bufferedImage, 0, 0, null);
        g.dispose();
        ImageIO.write(imageWithoutAlpha, suffix(), new File(path));
    }

    @Override
    public String suffix() {
        return "jpg";
    }
}
