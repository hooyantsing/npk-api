import xyz.hooy.npk.api.ApiService;
import xyz.hooy.npk.api.model.Index;
import xyz.hooy.npk.api.model.Texture;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class GetIndexsTest {

    public static void main(String[] args) throws IOException {
        ApiService apiService = ApiService.newInstance("/Users/hooy/Project/npk-loader/input/sprite_map_npc_chn_knight.NPK");
        Map<String, List<Index>> imgs = apiService.getIndexs();
        for (Map.Entry<String, List<Index>> img : imgs.entrySet()) {
            System.out.println(img.getKey());
            for (Index index : img.getValue()) {
                if (index instanceof Texture) {
                    Texture texture = (Texture) index;
                    System.out.println(texture);
                }
            }
        }
    }
}
