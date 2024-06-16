package npkapi;

import org.junit.jupiter.api.Test;
import xyz.hooy.npkapi.io.*;
import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;
import java.util.List;

public class NpkApiTest {

    @Test
    void importer() throws IOException {
        List<Album> albums = Importer.newInstance().addReader(new PngSpriteReader("D:\\Project\\NpkApi\\test\\output")).loadAll();
        System.out.println(albums);
    }

    @Test
    void exporter() throws IOException {
        List<Album> albums = Importer.newInstance().addReader(new NpkReader("D:\\Project\\NpkApi\\test\\input")).loadAll();
        Exporter.newInstance(albums)
                .addWriter(new GifAlbumWriter("D:\\Project\\NpkApi\\test\\output\\gif"))
                // .addWriter(new OggAlbumWriter("D:\\Project\\NpkApi\\test\\output\\ogg"))
                .addWriter(new PngSpriteWriter("D:\\Project\\NpkApi\\test\\output\\png"))
                .addWriter(new JpegSpriteWriter("D:\\Project\\NpkApi\\test\\output\\jpg"))
                .saveAll();
    }
}
