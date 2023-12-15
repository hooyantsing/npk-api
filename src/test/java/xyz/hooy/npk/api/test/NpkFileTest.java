package xyz.hooy.npk.api.test;

import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.Test;
import xyz.hooy.npk.api.NpkFileFactory;
import xyz.hooy.npk.api.color.ColorFactory;
import xyz.hooy.npk.api.constant.SupportedImages;
import xyz.hooy.npk.api.entity.TextureEntity;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Ignore
public class NpkFileTest {

    @Test
    void toPng() throws IOException {
        Map<String, List<TextureEntity>> textureMaps = NpkFileFactory.newInstance("/Users/hooy/Project/npk-api/input/sprite_map_npc_chn_knight.NPK").transferTextures();
        for (Map.Entry<String, List<TextureEntity>> textureEntries : textureMaps.entrySet()) {
            String fileName = Paths.get(textureEntries.getKey()).getFileName().toString();
            for (int i = 0; i < textureEntries.getValue().size(); i++) {
                TextureEntity texture = textureEntries.getValue().get(i);
                BufferedImage bufferedImage = ColorFactory.decode(texture);
                ImageIO.write(bufferedImage, SupportedImages.PNG.name(), new File("/Users/hooy/Project/npk-api/output/" + fileName + "_" + i + ".png"));
            }
            System.out.println(fileName);
        }
    }
}
