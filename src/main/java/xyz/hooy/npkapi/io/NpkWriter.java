package xyz.hooy.npkapi.io;

import xyz.hooy.npkapi.npk.NpkCore;
import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class NpkWriter extends AbstractPackWriter {

    public NpkWriter(String path) {
        super(path);
    }

    @Override
    protected void writeSingleFile(Path path, List<Album> albums) throws IOException {
        NpkCore.save(path + "." + suffix(), albums);
    }

    @Override
    public String suffix() {
        return "npk";
    }
}
