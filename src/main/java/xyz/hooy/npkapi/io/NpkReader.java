package xyz.hooy.npkapi.io;

import xyz.hooy.npkapi.npk.NpkCore;
import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class NpkReader extends AbstractPackReader {

    public NpkReader(String path) {
        super(path);
    }

    @Override
    protected List<Album> readSingleFile(Path path) throws IOException {
        return NpkCore.load(path.toString());
    }

    @Override
    public String suffix() {
        return "npk";
    }
}
