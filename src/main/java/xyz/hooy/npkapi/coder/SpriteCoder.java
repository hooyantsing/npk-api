package xyz.hooy.npkapi.coder;

import xyz.hooy.npkapi.entity.Sprite;

import java.io.IOException;

public interface SpriteCoder extends Coder {

    Sprite load(String loadPath) throws IOException;

    void save(String savePath, Sprite sprite) throws IOException;
}
