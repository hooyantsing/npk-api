package xyz.hooy.npkapi.io.impl;

import xyz.hooy.npkapi.io.SpriteReader;
import xyz.hooy.npkapi.npk.entity.Sprite;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PngSpriteReader implements SpriteReader {
    @Override
    public Sprite read(String path) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(new File(path));
        Sprite sprite = new Sprite();
        sprite.setPicture(bufferedImage);
        return sprite;
    }

    @Override
    public String suffix() {
        return "png";
    }
}
