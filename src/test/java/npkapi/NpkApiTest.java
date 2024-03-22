package npkapi;


import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.Test;
import xyz.hooy.npkapi.NpkApi;
import xyz.hooy.npkapi.entity.Album;
import xyz.hooy.npkapi.entity.Sprite;

import java.io.IOException;
import java.util.List;

public class NpkApiTest {

    @Test
    @Ignore
    void load() throws IOException {
        List<Album> albums = NpkApi.load("/Users/hooy/Project/NpkApi/input/sprite_map_npc_chn_knight.NPK");
        for (Album album : albums) {
            List<Sprite> sprites = album.getSprites();
            for (Sprite sprite : sprites) {
                sprite.getPicture();
            }
        }
        System.out.println("OK");
    }

    @Test
    @Ignore
    void save() throws IOException {
        List<Album> albums = NpkApi.load("/Users/hooy/Project/NpkApi/input/sprite_map_npc_chn_knight.NPK");
        for (Album album : albums) {
            List<Sprite> sprites = album.getSprites();
            for (Sprite sprite : sprites) {
                sprite.getPicture();
            }
        }
        NpkApi.save("/Users/hooy/Project/NpkApi/input/save.NPK", albums, "png");
        System.out.println("OK");
    }
}
