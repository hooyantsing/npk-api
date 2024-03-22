package xyz.hooy.npkapi.coder;

import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.constant.AlbumSuffixModes;
import xyz.hooy.npkapi.entity.Album;
import xyz.hooy.npkapi.entity.Sprite;
import xyz.hooy.npkapi.util.BufferedImageUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class GifAlbumCoder implements AlbumCoder {

    @Override
    public Album load(String loadPath) throws IOException {
        List<BufferedImage> bufferedImages = BufferedImageUtils.readImage(loadPath);
        Album album = new Album(bufferedImages);
        log.info("Loaded file: {}.", loadPath);
        return album;
    }

    @Override
    public void save(String savePath, Album album) throws IOException {
        List<BufferedImage> bufferedImages = album.getSprites().stream().map(Sprite::getPicture).collect(Collectors.toList());
        BufferedImageUtils.writeImage(savePath, bufferedImages, suffix());
        log.info("Saved file: {}.", savePath);
    }

    @Override
    public AlbumSuffixModes support() {
        return AlbumSuffixModes.IMAGE;
    }

    @Override
    public String suffix() {
        return "gif";
    }
}
