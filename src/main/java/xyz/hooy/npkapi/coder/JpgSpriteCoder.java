package xyz.hooy.npkapi.coder;

import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.entity.Sprite;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Slf4j
public class JpgSpriteCoder extends PngSpriteCoder {

    @Override
    public void save(String savePath, Sprite sprite) throws IOException {
        BufferedImage bufferedImage = withoutAlpha(sprite.getPicture());
        ImageIO.write(bufferedImage,suffix(),new File(savePath));
        log.info("Saved file: {}.", savePath);
    }

    private BufferedImage withoutAlpha(BufferedImage bufferedImage) {
        BufferedImage imageWithoutAlpha = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imageWithoutAlpha.createGraphics();
        g.drawImage(bufferedImage, 0, 0, null);
        g.dispose();
        return imageWithoutAlpha;
    }

    @Override
    public String suffix() {
        return "jpg";
    }
}
