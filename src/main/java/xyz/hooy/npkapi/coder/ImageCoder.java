package xyz.hooy.npkapi.coder;

import lombok.SneakyThrows;
import xyz.hooy.npkapi.constant.SupportedImages;
import xyz.hooy.npkapi.entity.ImgEntity;
import xyz.hooy.npkapi.entity.TextureEntity;
import xyz.hooy.npkapi.util.BufferedImageUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public final class ImageCoder {

    private ImageCoder() {
    }

    @SneakyThrows
    public static ImgEntity load(String path) {
        List<BufferedImage> bufferedImages = BufferedImageUtils.readImage(path);
        return new ImgEntity(bufferedImages);
    }

    @SneakyThrows
    public static void save(String path, List<ImgEntity> imgEntities, SupportedImages supportedImage) {
        for (ImgEntity imgEntity : imgEntities) {
            save(path, imgEntity, supportedImage);
        }
    }

    @SneakyThrows
    public static void save(String path, ImgEntity imgEntity, SupportedImages supportedImage) {
        if (supportedImage == SupportedImages.GIF) {
            saveGif(path, imgEntity);
        } else {
            for (TextureEntity textureEntity : imgEntity.getTextureEntities()) {
                save(path, textureEntity, supportedImage);
            }
        }
    }

    @SneakyThrows
    public static void save(String path, TextureEntity textureEntity, SupportedImages supportedImage) {
        BufferedImage bufferedImage = textureEntity.getPicture();
        String pathName = textureEntity.getParent().getPath();
        String imageName = pathName.substring(0, pathName.indexOf('.')).replace("/", "_") + "_" + textureEntity.getIndex() + "." + supportedImage.name().toLowerCase();
        BufferedImageUtils.writeImage(Paths.get(path, imageName).toString(), bufferedImage, supportedImage);
    }

    private static void saveGif(String path, ImgEntity imgEntity) throws IOException {
        List<BufferedImage> bufferedImages = imgEntity.getTextureEntities().stream().map(TextureEntity::getPicture).collect(Collectors.toList());
        String pathName = imgEntity.getPath();
        String imageName = pathName.substring(0, pathName.indexOf('.')).replace("/", "_") + "." + SupportedImages.GIF.name().toLowerCase();
        BufferedImageUtils.writeImage(Paths.get(path, imageName).toString(), bufferedImages, SupportedImages.GIF);
    }
}
