package xyz.hooy.npkapi.io;

import xyz.hooy.npkapi.npk.NpkCore;
import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class NpkWriter extends AbstractWriter{

    public NpkWriter(String path) {
        super(path);
    }

    @Override
    protected void write(Path path, List<Album> albums) throws IOException {
        NpkCore.save(path.toString(), albums);
    }

    @Override
    public String suffix() {
        return "npk";
    }
}
