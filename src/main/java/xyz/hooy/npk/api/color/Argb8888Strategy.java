package xyz.hooy.npk.api.color;

import xyz.hooy.npk.api.model.Texture;
import xyz.hooy.npk.api.model.TextureAttribute;
import xyz.hooy.npk.api.util.ByteUtils;

import java.awt.image.BufferedImage;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
class Argb8888Strategy extends AbstractColorStrategy {

    @Override
    public BufferedImage process(Texture texture) {
        TextureAttribute attribute = texture.getTextureAttribute();
        BufferedImage bufferedImage = new BufferedImage(attribute.getWidth(), attribute.getHeight(), BufferedImage.TYPE_4BYTE_ABGR_PRE);
        byte[] tempBytes = new byte[4];
        for (int i = 0; i < attribute.getHeight(); i++) {
            for (int j = 0; j < attribute.getWidth(); j++) {
                tempBytes[0] = texture.getTexture()[i * attribute.getWidth() * 4 + j * 4 + 0]; // blue
                tempBytes[1] = texture.getTexture()[i * attribute.getWidth() * 4 + j * 4 + 1]; // green
                tempBytes[2] = texture.getTexture()[i * attribute.getWidth() * 4 + j * 4 + 2]; // red
                tempBytes[3] = texture.getTexture()[i * attribute.getWidth() * 4 + j * 4 + 3]; // alpha
                int data = ByteUtils.bytesToInt(tempBytes);
                bufferedImage.setRGB(j, i, data);
            }
        }
        return bufferedImage;
    }
}
