package xyz.hooy.npkapi.coder;

import xyz.hooy.npkapi.entity.TextureEntity;

import java.io.IOException;

public interface ThirdCoder extends Coder {

    TextureEntity load(String loadPath) throws IOException;

    void save(String savePath, TextureEntity textureEntity) throws IOException;
}
