package xyz.hooy.npkapi.io;

import xyz.hooy.npkapi.npk.entity.Sprite;

import java.io.IOException;

public interface SpriteWriter {

    void write(String path, Sprite sprite) throws IOException;

    String suffix();
}
