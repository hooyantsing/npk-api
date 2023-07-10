package xyz.hooy.npk.api.color;

import xyz.hooy.npk.api.entity.TextureEntity;

import java.awt.image.BufferedImage;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
abstract class AbstractColorStrategy {

    public abstract BufferedImage process(TextureEntity texture);
}
