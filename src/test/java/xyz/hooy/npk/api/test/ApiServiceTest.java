package xyz.hooy.npk.api.test;

import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.Test;
import xyz.hooy.npk.api.ApiService;
import xyz.hooy.npk.api.model.AbstractIndex;
import xyz.hooy.npk.api.model.Texture;
import xyz.hooy.npk.api.util.TextureUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Ignore
public class ApiServiceTest {

    private static final String INPUT_PATH = "/Users/hooy/Project/npk-loader/input/";
    private static final String OUTPUT_PATH = "/Users/hooy/Project/npk-api/output/";
    private static final String INPUT_NPK_FILE = INPUT_PATH + "sprite_map_npc_chn_knight.NPK";

    @Test
    void getImg() throws IOException {
        ApiService apiService = ApiService.newInstance(INPUT_NPK_FILE);
        Map<String, byte[]> img = apiService.getImgs();
        for (String imgName : img.keySet()) {
            System.out.println(imgName);
        }
    }

    @Test
    void getIndexs() throws IOException {
        ApiService apiService = ApiService.newInstance(INPUT_NPK_FILE);
        Map<String, List<AbstractIndex>> imgs = apiService.getIndexs();
        for (Map.Entry<String, List<AbstractIndex>> img : imgs.entrySet()) {
            System.out.println(img.getKey());
            for (AbstractIndex index : img.getValue()) {
                if (index instanceof Texture) {
                    Texture texture = (Texture) index;
                    System.out.println(texture);
                }
            }
        }
    }

    @Test
    void toPng() throws IOException {
        ApiService apiService = ApiService.newInstance(INPUT_NPK_FILE);
        Map<String, List<AbstractIndex>> imgs = apiService.getIndexs();
        for (Map.Entry<String, List<AbstractIndex>> img : imgs.entrySet()) {
            Path fileName = Paths.get(img.getKey()).getFileName();
            System.out.println(fileName);
            for (int i = 0; i < img.getValue().size(); i++) {
                AbstractIndex index = img.getValue().get(i);
                if (index instanceof Texture) {
                    Texture texture = (Texture) index;
                    TextureUtils.toPng(texture, OUTPUT_PATH + fileName + "_" + i + ".png");
                }
            }
        }
    }
}
