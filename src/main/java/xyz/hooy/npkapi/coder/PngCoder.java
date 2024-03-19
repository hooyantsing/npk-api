package xyz.hooy.npkapi.coder;

import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.entity.ImgEntity;
import xyz.hooy.npkapi.entity.TextureEntity;
import xyz.hooy.npkapi.util.BufferedImageUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class PngCoder implements Coder {
    @Override
    public List<ImgEntity> load(List<String> loadPaths) throws IOException {
        Map<String, List<BufferedImage>> imgBufferedImages = new LinkedHashMap<>();
        for (String loadPath : loadPaths) {
            String fileName = Paths.get(loadPath).getFileName().toString();
            String imgPath = fileName.replace('_', '/').substring(0, fileName.lastIndexOf('-')) + ".img";
            BufferedImage bufferedImage = BufferedImageUtils.readImage(loadPath).get(0);
            List<BufferedImage> bufferedImages = imgBufferedImages.computeIfAbsent(imgPath, img -> new ArrayList<>());
            bufferedImages.add(bufferedImage);
            log.info("Loaded file: {}", loadPath);
        }
        List<ImgEntity> imgs = new ArrayList<>();
        for (Map.Entry<String, List<BufferedImage>> imgBufferedImage : imgBufferedImages.entrySet()) {
            List<BufferedImage> bufferedImages = imgBufferedImage.getValue();
            ImgEntity imgEntity = new ImgEntity(bufferedImages);
            imgEntity.setPath(imgBufferedImage.getKey());
            imgs.add(imgEntity);
        }
        return imgs;
    }

    @Override
    public void save(String savePath, List<ImgEntity> imgEntities) throws IOException {
        for (ImgEntity imgEntity : imgEntities) {
            List<TextureEntity> textureEntities = imgEntity.getTextureEntities();
            for (TextureEntity textureEntity : textureEntities) {
                BufferedImage bufferedImage = textureEntity.getPicture();
                String pathName = textureEntity.getParent().getPath();
                String imageName = pathName.substring(0, pathName.indexOf('.')).replace('/', '_') + "-" + textureEntity.getIndex() + ".png";
                String savedPath = Paths.get(savePath, imageName).toString();
                BufferedImageUtils.writeImage(savedPath, bufferedImage, getSuffix());
                log.info("Saved file: {}", savedPath);
            }
        }
    }

    @Override
    public String getSuffix() {
        return "png";
    }
}
