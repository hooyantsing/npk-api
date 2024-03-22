package xyz.hooy.npkapi.coder;

import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.entity.Sprite;
import xyz.hooy.npkapi.util.BufferedImageUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

@Slf4j
public class PngSpriteCoder implements SpriteCoder {

    @Override
    public Sprite load(String loadPath) throws IOException {
        List<BufferedImage> bufferedImages = BufferedImageUtils.readImage(loadPath);
        Sprite sprite = new Sprite();
        sprite.setPicture(bufferedImages.get(0));
        log.info("Loaded file: {}.", loadPath);
        return sprite;
    }

    @Override
    public void save(String savePath, Sprite sprite) throws IOException {
        BufferedImage bufferedImage = sprite.getPicture();
        BufferedImageUtils.writeImage(savePath, bufferedImage, suffix());
        log.info("Saved file: {}.", savePath);
    }

    @Override
    public String suffix() {
        return "png";
    }
}
