package xyz.hooy.npkapi.io;

import xyz.hooy.npkapi.npk.entity.Sprite;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public class JpegSpriteWriter extends AbstractSpriteWriter{

    public JpegSpriteWriter(String path) {
        super(path);
    }

    @Override
    protected void write(Path path, Sprite sprite) throws IOException {
        BufferedImage bufferedImage = sprite.getPicture();
        BufferedImage imageWithoutAlpha = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imageWithoutAlpha.createGraphics();
        g.drawImage(bufferedImage, 0, 0, null);
        g.dispose();
        ImageIO.write(imageWithoutAlpha, suffix(), path.toFile());
    }

    @Override
    public String suffix() {
        return "jpg";
    }
}
