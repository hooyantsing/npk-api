package xyz.hooy.npk.api.color;

import xyz.hooy.npk.api.entity.TextureEntity;

import java.awt.image.BufferedImage;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
abstract class AbstractColorConverter {

    /**
     * 解码
     */
    public abstract BufferedImage decode(TextureEntity texture);

    /**
     * 编码
     */
    public abstract TextureEntity encode(BufferedImage bufferedImage);
}
