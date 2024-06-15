package npkapi;

import org.junit.jupiter.api.Test;
import xyz.hooy.npkapi.io.*;
import xyz.hooy.npkapi.io.io.*;
import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;
import java.util.List;

public class NpkApiTest {

    @Test
    void importer() throws IOException {
        List<Album> albums = Importer.newInstance().addReader(new GifAlbumReader("")).addReader(new PngSpriteReader("")).loadAll();
    }

    void exporter() throws IOException {
        List<Album> albums = Importer.newInstance().addReader(new GifAlbumReader("")).loadAll();
        Exporter.newInstance(albums).addWriter(new GifAlbumWriter("")).addWriter(new PngSpriteWriter("")).saveAll();
    }
}
