package xyz.hooy.npkapi.io;

import xyz.hooy.npkapi.npk.entity.Sprite;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public class PngSpriteReader extends AbstractSpriteReader {

    public PngSpriteReader(String path) {
        super(path);
    }

    @Override
    protected Sprite read(Path singleFile) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(path.toFile());
        Sprite sprite = new Sprite();
        sprite.setPicture(bufferedImage);
        return sprite;
    }

    @Override
    public String suffix() {
        return "png";
    }
}
