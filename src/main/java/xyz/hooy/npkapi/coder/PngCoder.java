package xyz.hooy.npkapi.coder;

import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.entity.ImgEntity;
import xyz.hooy.npkapi.entity.TextureEntity;
import xyz.hooy.npkapi.util.BufferedImageUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

@Slf4j
public class PngCoder implements ThirdCoder {

    @Override
    public TextureEntity load(String loadPath) throws IOException {
        List<BufferedImage> bufferedImages = BufferedImageUtils.readImage(loadPath);
        ImgEntity imgEntity = new ImgEntity(bufferedImages);
        TextureEntity textureEntity = imgEntity.getTextureEntities().get(0);
        log.info("Loaded file: {}.", loadPath);
        return textureEntity;
    }

    @Override
    public void save(String savePath, TextureEntity textureEntity) throws IOException {
        BufferedImage bufferedImage = textureEntity.getPicture();
        BufferedImageUtils.writeImage(savePath, bufferedImage, suffix());
        log.info("Saved file: {}.", savePath);
    }

    @Override
    public String suffix() {
        return "png";
    }
}
