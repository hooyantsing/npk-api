package xyz.hooy.npk.api.color;

import xyz.hooy.npk.api.constant.ColorLinkTypes;
import xyz.hooy.npk.api.entity.TextureEntity;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
public final class ColorConverter {

    private static final Map<ColorLinkTypes, AbstractConverter> colorMap = new HashMap<>();

    static {
        register(ColorLinkTypes.ARGB1555, new Argb1555Converter());
        register(ColorLinkTypes.ARGB4444, new Argb4444Converter());
        register(ColorLinkTypes.ARGB8888, new Argb8888Converter());
    }

    private static void register(ColorLinkTypes type, AbstractConverter strategy) {
        colorMap.put(type, strategy);
    }

    private static AbstractConverter get(ColorLinkTypes type) {
        AbstractConverter abstractConverter = colorMap.get(type);
        if (Objects.isNull(abstractConverter)) {
            throw new UnsupportedOperationException(String.format("The current type is not supported %s", type));
        }
        return abstractConverter;
    }

    public static BufferedImage decode(TextureEntity texture) {
        return get(texture.getTextureAttribute().getType()).decode(texture);
    }

    public static TextureEntity encode(BufferedImage bufferedImage, ColorLinkTypes type) {
        return get(type).encode(bufferedImage);
    }
}
