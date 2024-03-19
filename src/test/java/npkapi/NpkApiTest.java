package npkapi;


import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.Test;
import xyz.hooy.npkapi.NpkApi;
import xyz.hooy.npkapi.entity.ImgEntity;
import xyz.hooy.npkapi.entity.TextureEntity;

import java.io.IOException;
import java.util.List;

public class NpkApiTest {

    @Test
    @Ignore
    void load() throws IOException {
        List<ImgEntity> imgEntities = NpkApi.load("/Users/hooy/Project/NpkApi/input/sprite_map_npc_chn_knight.NPK");
        for (ImgEntity imgEntity : imgEntities) {
            List<TextureEntity> textureEntities = imgEntity.getTextureEntities();
            for (TextureEntity textureEntity : textureEntities) {
                textureEntity.getPicture();
            }
        }
        System.out.println("OK");
    }

    @Test
    @Ignore
    void save() throws IOException {
        List<ImgEntity> imgEntities = NpkApi.load("/Users/hooy/Project/NpkApi/input/sprite_map_npc_chn_knight.NPK");
        for (ImgEntity imgEntity : imgEntities) {
            List<TextureEntity> textureEntities = imgEntity.getTextureEntities();
            for (TextureEntity textureEntity : textureEntities) {
                textureEntity.getPicture();
            }
        }
        NpkApi.save("/Users/hooy/Project/NpkApi/input/save.NPK", imgEntities, "png");
        System.out.println("OK");
    }
}
