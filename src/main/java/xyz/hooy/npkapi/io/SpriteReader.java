package xyz.hooy.npkapi.io;

import xyz.hooy.npkapi.npk.entity.Sprite;

import java.io.IOException;

public interface SpriteReader {

    Sprite read(String path) throws IOException;

    String suffix();
}
