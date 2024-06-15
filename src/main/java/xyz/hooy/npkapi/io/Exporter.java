package xyz.hooy.npkapi.io;

import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Exporter {

    private final List<AbstractWriter> writers = new ArrayList<>();

    private final List<Album> albums;

    private Exporter(List<Album> albums) {
        this.albums = albums;
    }

    public static Exporter newInstance(List<Album> albums) {
        return new Exporter(albums);
    }

    public Exporter addWriter(AbstractWriter writer) {
        writers.add(writer);
        return this;
    }

    public void saveAll() throws IOException {
        for (AbstractWriter writer : writers) {
            writer.write(albums);
        }
    }
}
