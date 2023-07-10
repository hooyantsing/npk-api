package xyz.hooy.npk.api;

import java.io.IOException;

public class NpkFileFactory {

    public static NpkFile newInstance(String path) throws IOException {
        return new NpkFile(path);
    }
}
