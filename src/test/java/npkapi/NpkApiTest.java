package npkapi;


import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.Test;
import xyz.hooy.npkapi.coder.NpkCoder;
import xyz.hooy.npkapi.entity.ImgEntity;
import xyz.hooy.npkapi.entity.TextureEntity;

import java.util.List;

public class NpkApiTest {

    @Test
    @Ignore
    void test() {
        List<ImgEntity> imgEntities = NpkCoder.load(false, "/Users/hooy/Project/NpkApi/input/sprite_map_act2_stoneimage.NPK");
        for (ImgEntity imgEntity : imgEntities) {
            List<TextureEntity> textureEntities = imgEntity.getTextureEntities();
            for (TextureEntity textureEntity : textureEntities) {
                textureEntity.getPicture();
            }
        }
        System.out.println("OK");
    }
}
