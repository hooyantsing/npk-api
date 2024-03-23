package npkapi;


import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.Test;
import xyz.hooy.npkapi.NpkApi;
import xyz.hooy.npkapi.entity.Album;

import java.io.IOException;
import java.util.List;

public class NpkApiTest {

    @Test
    @Ignore
    void saveNpkToNpk() throws IOException {
        List<Album> albums = NpkApi.load("D:\\Project\\NpkApi\\test\\input\\sprite_map_act2_stoneimage.NPK");
        NpkApi.save("D:\\Project\\NpkApi\\test\\output\\npk", albums, "npk");
        System.out.println("OK");
    }

    @Test
    @Ignore
    void saveNpkToGif() throws IOException {
        List<Album> albums = NpkApi.load("D:\\Project\\NpkApi\\test\\input\\sprite_map_act2_stoneimage.NPK");
        NpkApi.save("D:\\Project\\NpkApi\\test\\output\\gif", albums, "gif");
        System.out.println("OK");
    }

    @Test
    @Ignore
    void saveNpkToPng() throws IOException {
        List<Album> albums = NpkApi.load("D:\\Project\\NpkApi\\test\\input\\sprite_map_act2_stoneimage.NPK");
        NpkApi.save("D:\\Project\\NpkApi\\test\\output\\png", albums, "png");
        System.out.println("OK");
    }

    @Test
    @Ignore
    void saveNpkToOgg() throws IOException {
        List<Album> albums = NpkApi.load("D:\\Project\\NpkApi\\test\\input\\sounds_amb.npk");
        NpkApi.save("D:\\Project\\NpkApi\\test\\output\\ogg", albums, "ogg");
        System.out.println("OK");
    }

    @Test
    @Ignore
    void saveAllToNpk() throws IOException {
        List<Album> albums = NpkApi.load("D:\\Project\\NpkApi\\test\\output");
        NpkApi.save("D:\\Project\\NpkApi\\test\\output\\all", albums, "npk");
        System.out.println("OK");
    }
}
