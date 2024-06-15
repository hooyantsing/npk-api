package xyz.hooy.npkapi.io;

import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Importer {

    private final List<AbstractReader> readers = new ArrayList<>();

    private Importer() {
    }

    public static Importer newInstance() {
        return new Importer();
    }

    public Importer addReader(AbstractReader reader) {
        readers.add(reader);
        return this;
    }

    public List<Album> loadAll() throws IOException {
        List<Album> albums = new ArrayList<>();
        for (AbstractReader reader : readers) {
            albums.addAll(reader.read());
        }
        return albums;
    }
}
