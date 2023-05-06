package xyz.hooy.npk.api.test;

import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import xyz.hooy.npk.api.ApiService;
import xyz.hooy.npk.api.model.AbstractIndex;
import xyz.hooy.npk.api.model.Texture;
import xyz.hooy.npk.api.util.TextureUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Ignore
public class ApiServiceTest {

    private static final String INPUT_PATH = "/Users/hooy/Project/npk-loader/input/";
    private static final String OUTPUT_PATH = "/Users/hooy/Project/npk-api/output/";
    private static final String INPUT_NPK_FILE = INPUT_PATH + "sprite_map_npc_chn_knight.NPK";

    @Test
    void getImgs() throws IOException {
        Map<String, byte[]> imgs = ApiService.newInstance(INPUT_NPK_FILE).getImgs();
        long count = imgs.keySet().stream().peek(System.out::println).count();
        Assertions.assertEquals(count, 11);
    }

    @Test
    void getIndexs() throws IOException {
        Map<String, List<AbstractIndex>> indexs = ApiService.newInstance(INPUT_NPK_FILE).getIndexs();
        indexs.entrySet().forEach(entry -> {
            System.out.println(entry.getKey());
            entry.getValue().stream().forEach(System.out::println);
        });
    }

    @Test
    void transferTextures() throws IOException {
        Map<String, List<Texture>> textures = ApiService.newInstance(INPUT_NPK_FILE).transferTextures();
        textures.entrySet().forEach(entry -> {
            System.out.println(entry.getKey());
            entry.getValue().stream().forEach(System.out::println);
        });
    }

    @Test
    void addImg() throws IOException {
        byte[] img = Files.readAllBytes(Paths.get(INPUT_PATH + "modred.img"));
        byte[] build = ApiService.newInstance(INPUT_NPK_FILE).addImg(img, "img/modred/hooy.img").build();
        Files.write(Paths.get(OUTPUT_PATH + "ApiService-addImg-test.npk"), build);
    }

    @Test
    void removeImg() throws IOException {
        byte[] build = ApiService.newInstance(INPUT_NPK_FILE).removeImg(1).build();
        Files.write(Paths.get(OUTPUT_PATH + "ApiService-removeImg-test.npk"), build);
    }

    @Test
    void removeIndex() throws IOException {
        byte[] build = ApiService.newInstance(INPUT_NPK_FILE).removeIndex(1, 2).build();
        Files.write(Paths.get(OUTPUT_PATH + "ApiService-removeIndex-test.npk"), build);
    }

    @Test
    void toPng() throws IOException {
        Map<String, List<AbstractIndex>> indexs = ApiService.newInstance(INPUT_NPK_FILE).getIndexs();
        for (Map.Entry<String, List<AbstractIndex>> entry : indexs.entrySet()) {
            String fileName = Paths.get(entry.getKey()).getFileName().toString();
            for (int i = 0; i < entry.getValue().size(); i++) {
                AbstractIndex index = entry.getValue().get(i);
                if (index instanceof Texture) {
                    TextureUtils.toPng((Texture) index, OUTPUT_PATH + fileName + "_" + i + ".png");
                }
            }
            System.out.println(fileName);
        }
    }

}
