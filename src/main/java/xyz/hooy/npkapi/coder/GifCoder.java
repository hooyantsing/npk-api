package xyz.hooy.npkapi.coder;

import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.entity.ImgEntity;
import xyz.hooy.npkapi.entity.TextureEntity;
import xyz.hooy.npkapi.util.BufferedImageUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class GifCoder implements Coder {
    @Override
    public List<ImgEntity> load(List<String> loadPaths) throws IOException {
        List<ImgEntity> imgs = new ArrayList<>();
        for (String loadPath : loadPaths) {
            List<BufferedImage> bufferedImages = BufferedImageUtils.readImage(loadPath);
            ImgEntity imgEntity = new ImgEntity(bufferedImages);
            String path = loadPath.substring(0, loadPath.lastIndexOf('.')).replace('_', '/') + ".img";
            imgEntity.setPath(path);
            imgs.add(imgEntity);
            log.info("Loaded file: {}.", loadPath);
        }
        return imgs;
    }

    @Override
    public void save(String savePath, List<ImgEntity> imgEntities) throws IOException {
        for (ImgEntity imgEntity : imgEntities) {
            List<BufferedImage> bufferedImages = imgEntity.getTextureEntities().stream().map(TextureEntity::getPicture).collect(Collectors.toList());
            String pathName = imgEntity.getPath();
            String imageName = pathName.substring(0, pathName.indexOf('.')).replace('/', '_') + "." + getSuffix();
            String savedPath = Paths.get(savePath, imageName).toString();
            BufferedImageUtils.writeImage(savedPath, bufferedImages, getSuffix());
            log.info("Saved file: {}.", savedPath);
        }
    }

    @Override
    public String getSuffix() {
        return "gif";
    }
}
