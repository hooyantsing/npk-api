package xyz.hooy.npk.api.color;

import xyz.hooy.npk.api.entity.TextureEntity;
import xyz.hooy.npk.api.entity.TextureAttribute;
import xyz.hooy.npk.api.util.ByteUtils;

import java.awt.image.BufferedImage;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
class Argb4444Strategy extends AbstractColorStrategy {

    @Override
    public BufferedImage process(TextureEntity texture) {
        TextureAttribute attribute = texture.getTextureAttribute();
        BufferedImage bufferedImage = new BufferedImage(attribute.getWidth(), attribute.getHeight(), BufferedImage.TYPE_4BYTE_ABGR_PRE);
        byte[] tempBytes = new byte[4];
        for (int i = 0; i < attribute.getHeight(); i++) {
            for (int j = 0; j < attribute.getWidth(); j++) {
                tempBytes[0] = (byte) ((texture.getTexture()[i * attribute.getWidth() * 2 + j * 2 + 0] & 0x0F) << 4); // blue
                tempBytes[1] = (byte) (((texture.getTexture()[i * attribute.getWidth() * 2 + j * 2 + 0] & 0xF0) >> 4) << 4); // green
                tempBytes[2] = (byte) ((texture.getTexture()[i * attribute.getWidth() * 2 + j * 2 + 1] & 0x0F) << 4);   // red
                tempBytes[3] = (byte) (((texture.getTexture()[i * attribute.getWidth() * 2 + j * 2 + 1] & 0xF0) >> 4) << 4); // alpha
                int data = ByteUtils.bytesToInt(tempBytes);
                bufferedImage.setRGB(j, i, data);
            }
        }
        return bufferedImage;
    }
}
