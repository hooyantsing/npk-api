package xyz.hooy.npk.api.color;

import xyz.hooy.npk.api.model.Texture;

import java.awt.image.BufferedImage;

abstract class AbstractColorStrategy {

    public abstract BufferedImage process(Texture texture);
}
