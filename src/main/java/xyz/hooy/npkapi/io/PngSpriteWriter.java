package xyz.hooy.npkapi.io;

import xyz.hooy.npkapi.npk.entity.Sprite;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public class PngSpriteWriter extends AbstractSpriteWriter {

    public PngSpriteWriter(String path) {
        super(path);
    }

    @Override
    protected void writeSingleFile(Path path, Sprite sprite) throws IOException {
        BufferedImage bufferedImage = sprite.getPicture();
        ImageIO.write(bufferedImage, suffix(), path.toFile());
    }

    @Override
    public String suffix() {
        return "png";
    }
}
