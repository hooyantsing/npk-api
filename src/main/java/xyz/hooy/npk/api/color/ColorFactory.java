package xyz.hooy.npk.api.color;

import xyz.hooy.npk.api.constant.IndexConstant;
import xyz.hooy.npk.api.entity.TextureEntity;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
public final class ColorFactory {

    private static final Map<Integer, AbstractColorStrategy> colorMap = new HashMap<>();

    static {
        register(IndexConstant.TYPE_TEXTURE_ARGB1555, new Argb1555Strategy());
        register(IndexConstant.TYPE_TEXTURE_ARGB4444, new Argb4444Strategy());
        register(IndexConstant.TYPE_TEXTURE_ARGB8888, new Argb8888Strategy());
    }

    private static void register(int type, AbstractColorStrategy strategy) {
        colorMap.put(type, strategy);
    }

    private static AbstractColorStrategy get(int type) {
        AbstractColorStrategy abstractColorStrategy = colorMap.get(type);
        if (abstractColorStrategy == null) {
            throw new UnsupportedOperationException(String.format("The current type is not supported %s", type));
        }
        return abstractColorStrategy;
    }

    public static BufferedImage process(TextureEntity texture) {
        return get(texture.getTextureAttribute().getType()).process(texture);
    }
}
