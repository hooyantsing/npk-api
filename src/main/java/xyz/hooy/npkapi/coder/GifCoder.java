package xyz.hooy.npkapi.coder;

import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.constant.ImgType;
import xyz.hooy.npkapi.entity.ImgEntity;
import xyz.hooy.npkapi.entity.TextureEntity;
import xyz.hooy.npkapi.util.BufferedImageUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class GifCoder implements SecondCoder {

    @Override
    public ImgEntity load(String loadPath) throws IOException {
        List<BufferedImage> bufferedImages = BufferedImageUtils.readImage(loadPath);
        ImgEntity imgEntity = new ImgEntity(bufferedImages);
        log.info("Loaded file: {}.", loadPath);
        return imgEntity;
    }

    @Override
    public void save(String savePath, ImgEntity imgEntity) throws IOException {
        List<BufferedImage> bufferedImages = imgEntity.getTextureEntities().stream().map(TextureEntity::getPicture).collect(Collectors.toList());
        BufferedImageUtils.writeImage(savePath, bufferedImages, suffix());
        log.info("Saved file: {}.", savePath);
    }

    @Override
    public boolean match(ImgType imgType) {
        return ImgType.IMAGE == imgType;
    }

    @Override
    public String suffix() {
        return "gif";
    }
}
