package xyz.hooy.npkapi.io;

import xyz.hooy.npkapi.npk.NpkCore;
import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;
import java.util.List;

public class NpkReader extends AbstractReader{

    public NpkReader(String path) {
        super(path);
    }

    @Override
    public List<Album> read() throws IOException {
        return NpkCore.load(path.toString());
    }

    @Override
    public String suffix() {
        return "npk";
    }
}
