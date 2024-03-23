package npkapi;


import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import xyz.hooy.npkapi.NpkApi;
import xyz.hooy.npkapi.entity.Album;

import java.io.IOException;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NpkApiTest {

    @Test
    @Ignore
    @Order(1)
    void saveNpkToNpk() throws IOException {
        List<Album> albums = NpkApi.load("D:\\Project\\NpkApi\\test\\input\\sprite_map_act2_stoneimage.NPK");
        NpkApi.save("D:\\Project\\NpkApi\\test\\output\\npk", albums, "npk");
        System.out.println("OK");
    }

    @Test
    @Ignore
    @Order(2)
    void saveNpkToGif() throws IOException {
        List<Album> albums = NpkApi.load("D:\\Project\\NpkApi\\test\\input\\sprite_map_act2_stoneimage.NPK");
        NpkApi.save("D:\\Project\\NpkApi\\test\\output\\gif", albums, "gif");
        System.out.println("OK");
    }

    @Test
    @Ignore
    @Order(3)
    void saveNpkToPng() throws IOException {
        List<Album> albums = NpkApi.load("D:\\Project\\NpkApi\\test\\input\\sprite_map_act2_stoneimage.NPK");
        NpkApi.save("D:\\Project\\NpkApi\\test\\output\\png", albums, "png");
        System.out.println("OK");
    }

    @Test
    @Ignore
    @Order(4)
    void saveNpkToJpg() throws IOException {
        List<Album> albums = NpkApi.load("D:\\Project\\NpkApi\\test\\input\\sprite_map_act2_stoneimage.NPK");
        NpkApi.save("D:\\Project\\NpkApi\\test\\output\\jpg", albums, "jpg");
        System.out.println("OK");
    }

    @Test
    @Ignore
    @Order(5)
    void saveNpkToOgg() throws IOException {
        List<Album> albums = NpkApi.load("D:\\Project\\NpkApi\\test\\input\\sounds_amb.npk");
        NpkApi.save("D:\\Project\\NpkApi\\test\\output\\ogg", albums, "ogg");
        System.out.println("OK");
    }

    @Test
    @Ignore
    @Order(6)
    void saveAllToNpk() throws IOException {
        List<Album> albums = NpkApi.load("D:\\Project\\NpkApi\\test\\output");
        NpkApi.save("D:\\Project\\NpkApi\\test\\output\\all", albums, "npk");
        System.out.println("OK");
    }
}
