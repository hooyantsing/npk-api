package xyz.hooy.npk.api.color;

import xyz.hooy.npk.api.constant.IndexConstant;
import xyz.hooy.npk.api.entity.TextureAttribute;
import xyz.hooy.npk.api.entity.TextureEntity;
import xyz.hooy.npk.api.util.ByteUtils;

import java.awt.image.BufferedImage;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
class Argb8888Strategy extends AbstractColorStrategy {

    @Override
    public BufferedImage decode(TextureEntity texture) {
        TextureAttribute attribute = texture.getTextureAttribute();
        BufferedImage bufferedImage = new BufferedImage(attribute.getWidth(), attribute.getHeight(), BufferedImage.TYPE_4BYTE_ABGR_PRE);
        byte[] tempBytes = new byte[4];
        for (int i = 0; i < attribute.getHeight(); i++) {
            for (int j = 0; j < attribute.getWidth(); j++) {
                tempBytes[0] = texture.getTexture()[i * attribute.getWidth() * 4 + j * 4]; // blue
                tempBytes[1] = texture.getTexture()[i * attribute.getWidth() * 4 + j * 4 + 1]; // green
                tempBytes[2] = texture.getTexture()[i * attribute.getWidth() * 4 + j * 4 + 2]; // red
                tempBytes[3] = texture.getTexture()[i * attribute.getWidth() * 4 + j * 4 + 3]; // alpha
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
        attribute.setCompress(IndexConstant.TEXTURE_NON_ZLIB);
        attribute.setHeight(bufferedImage.getHeight());
        attribute.setWidth(bufferedImage.getWidth());
        byte[] textureBytes = new byte[bufferedImage.getHeight() * bufferedImage.getWidth() * 4];
        for (int i = 0; i < bufferedImage.getHeight(); i++) {
            for (int j = 0; j < bufferedImage.getWidth(); j++) {
                int data = bufferedImage.getRGB(j, i);
                byte[] tempBytes = ByteUtils.intToBytes(data);
                textureBytes[i * bufferedImage.getWidth() * 4 + j * 4] = tempBytes[0]; // blue
                textureBytes[i * bufferedImage.getWidth() * 4 + j * 4 + 1] = tempBytes[1]; // green
                textureBytes[i * bufferedImage.getWidth() * 4 + j * 4 + 2] = tempBytes[2]; // red
                textureBytes[i * bufferedImage.getWidth() * 4 + j * 4 + 3] = tempBytes[3]; // alpha
            }
        }
        texture.setTexture(textureBytes);
        return texture;
    }
}
