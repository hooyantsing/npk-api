package xyz.hooy.npkapi.io.impl;

import xyz.hooy.npkapi.npk.NpkCore;
import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;
import java.util.List;

public class NpkReader {
    public List<Album> read(String path) throws IOException {
        return NpkCore.load(path);
    }

    public String suffix() {
        return "npk";
    }
}
