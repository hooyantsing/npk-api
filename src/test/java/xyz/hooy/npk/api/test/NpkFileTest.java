package xyz.hooy.npk.api.test;

import com.sun.prism.Texture;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.Test;
import xyz.hooy.npk.api.NpkFileFactory;
import xyz.hooy.npk.api.entity.AbstractIndex;
import xyz.hooy.npk.api.entity.TextureEntity;
import xyz.hooy.npk.api.util.TextureUtils;

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
                TextureEntity index = textureEntries.getValue().get(i);
                TextureUtils.toPng(index, "/Users/hooy/Project/npk-api/output/" + fileName + "_" + i + ".png");
            }
            System.out.println(fileName);
        }
    }
}
