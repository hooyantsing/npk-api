package xyz.hooy.npkapi.coder;

import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.entity.Sprite;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Slf4j
public class PngSpriteCoder implements SpriteCoder {

    @Override
    public Sprite load(String loadPath) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(new File(loadPath));
        Sprite sprite = new Sprite();
        sprite.setPicture(bufferedImage);
        log.info("Loaded file: {}.", loadPath);
        return sprite;
    }

    @Override
    public void save(String savePath, Sprite sprite) throws IOException {
        BufferedImage bufferedImage = sprite.getPicture();
        ImageIO.write(bufferedImage,suffix(),new File(savePath));
        log.info("Saved file: {}.", savePath);
    }

    @Override
    public String suffix() {
        return "png";
    }
}
