package npkapi;


import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.Test;
import xyz.hooy.npkapi.coder.NpkCoder;
import xyz.hooy.npkapi.entity.ImgEntity;
import xyz.hooy.npkapi.entity.TextureEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class NpkApiTest {

    @Test
    @Ignore
    void load() {
        List<ImgEntity> imgEntities = NpkCoder.load(false, "/Users/hooy/Project/NpkApi/input/sprite_map_npc_chn_knight.NPK");
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
    void save() {
        List<ImgEntity> imgEntities = NpkCoder.load(false, "/Users/hooy/Project/NpkApi/input/sprite_map_act2_stoneimage.NPK");
        for (ImgEntity imgEntity : imgEntities) {
            List<TextureEntity> textureEntities = imgEntity.getTextureEntities();
            for (TextureEntity textureEntity : textureEntities) {
                textureEntity.getPicture();
            }
        }
        NpkCoder.save("/Users/hooy/Project/NpkApi/input/save.NPK", imgEntities);
        System.out.println("OK");
    }

    @Test
    @Ignore
    void test() throws IOException {
        byte[] o = Files.readAllBytes(Paths.get("/Users/hooy/Project/NpkApi/input/sprite_map_act2_stoneimage.NPK"));
        System.out.println(Arrays.toString(o));
        byte[] t = Files.readAllBytes(Paths.get("/Users/hooy/Project/NpkApi/input/save.NPK"));
        System.err.println(Arrays.toString(t));
    }
}
