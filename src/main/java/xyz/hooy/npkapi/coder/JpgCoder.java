package xyz.hooy.npkapi.coder;

import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.entity.ImgEntity;
import xyz.hooy.npkapi.entity.TextureEntity;
import xyz.hooy.npkapi.util.BufferedImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
public class JpgCoder extends PngCoder implements Coder {
    @Override
    public List<ImgEntity> load(List<String> loadPaths) throws IOException {
        return super.load(loadPaths);
    }

    @Override
    public void save(String savePath, List<ImgEntity> imgEntities) throws IOException {
        for (ImgEntity imgEntity : imgEntities) {
            List<TextureEntity> textureEntities = imgEntity.getTextureEntities();
            for (TextureEntity textureEntity : textureEntities) {
                BufferedImage bufferedImage = textureEntity.getPicture();
                bufferedImage = withoutAlpha(bufferedImage);
                String pathName = textureEntity.getParent().getPath();
                String imageName = pathName.substring(0, pathName.indexOf('.')).replace('/', '_') + "-" + textureEntity.getIndex() + "." + getSuffix();
                String savedPath = Paths.get(savePath, imageName).toString();
                BufferedImageUtils.writeImage(savedPath, bufferedImage, getSuffix());
                log.info("Saved file: {}.", savedPath);
            }
        }
    }

    private BufferedImage withoutAlpha(BufferedImage bufferedImage){
        BufferedImage imageWithoutAlpha = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imageWithoutAlpha.createGraphics();
        g.drawImage(bufferedImage, 0, 0, null);
        g.dispose();
        return imageWithoutAlpha;
    }

    @Override
    public String getSuffix() {
        return "jpg";
    }
}
