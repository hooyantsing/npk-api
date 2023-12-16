package xyz.hooy.npk.api.color;

import xyz.hooy.npk.api.constant.CompressModes;
import xyz.hooy.npk.api.entity.TextureAttribute;
import xyz.hooy.npk.api.entity.TextureEntity;
import xyz.hooy.npk.api.util.ByteUtils;

import java.awt.image.BufferedImage;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
class Argb4444Converter extends AbstractColorConverter {

    @Override
    public BufferedImage decode(TextureEntity texture) {
        TextureAttribute attribute = texture.getTextureAttribute();
        BufferedImage bufferedImage = new BufferedImage(attribute.getWidth(), attribute.getHeight(), BufferedImage.TYPE_4BYTE_ABGR_PRE);
        byte[] tempBytes = new byte[4];
        for (int i = 0; i < attribute.getHeight(); i++) {
            for (int j = 0; j < attribute.getWidth(); j++) {
                tempBytes[0] = (byte) ((texture.getTexture()[i * attribute.getWidth() * 2 + j * 2] & 0x0F) << 4); // blue
                tempBytes[1] = (byte) (((texture.getTexture()[i * attribute.getWidth() * 2 + j * 2] & 0xF0) >> 4) << 4); // green
                tempBytes[2] = (byte) ((texture.getTexture()[i * attribute.getWidth() * 2 + j * 2 + 1] & 0x0F) << 4);   // red
                tempBytes[3] = (byte) (((texture.getTexture()[i * attribute.getWidth() * 2 + j * 2 + 1] & 0xF0) >> 4) << 4); // alpha
                int data = ByteUtils.bytesToInt(tempBytes);
                bufferedImage.setRGB(j, i, data);
            }
        }
        return bufferedImage;
    }

    @Override
    public TextureEntity encode(BufferedImage bufferedImage) {
        TextureEntity texture = new TextureEntity();
        TextureAttribute attribute = texture.getTextureAttribute();
        attribute.setCompress(CompressModes.NONE);
        attribute.setHeight(bufferedImage.getHeight());
        attribute.setWidth(bufferedImage.getWidth());
        byte[] textureBytes = new byte[bufferedImage.getHeight() * bufferedImage.getWidth() * 2];
        for (int y = 0; y < bufferedImage.getHeight(); y++) {
            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                int argb = bufferedImage.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                int red = (argb >> 16) & 0xFF;
                int green = (argb >> 8) & 0xFF;
                int blue = argb & 0xFF;
                int argb4444 = ((alpha >> 4) << 12) | ((red >> 4) << 8) | ((green >> 4) << 4) | (blue >> 4);
                textureBytes[(y * bufferedImage.getWidth() + x) * 2] = (byte) (argb4444 & 0xFF);
                textureBytes[(y * bufferedImage.getWidth() + x) * 2 + 1] = (byte) ((argb4444 >> 8) & 0xFF);
            }
        }
        texture.setTexture(textureBytes);
        return texture;
    }
}
