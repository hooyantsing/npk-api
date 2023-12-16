package xyz.hooy.npk.api.color;

import xyz.hooy.npk.api.constant.CompressModes;
import xyz.hooy.npk.api.entity.TextureEntity;
import xyz.hooy.npk.api.util.ByteUtils;

import java.awt.image.BufferedImage;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
class Argb8888Converter extends AbstractColorConverter {

    @Override
    public BufferedImage decode(TextureEntity texture) {
        int width = texture.getWidth();
        int height = texture.getHeight();
        byte[] textureData = texture.getTextureData();
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR_PRE);
        byte[] tempBytes = new byte[4];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                tempBytes[0] = textureData[i * width * 4 + j * 4]; // blue
                tempBytes[1] = textureData[i * width * 4 + j * 4 + 1]; // green
                tempBytes[2] = textureData[i * width * 4 + j * 4 + 2]; // red
                tempBytes[3] = textureData[i * width * 4 + j * 4 + 3]; // alpha
                int data = ByteUtils.bytesToInt(tempBytes);
                bufferedImage.setRGB(j, i, data);
            }
        }
        return bufferedImage;
    }

    @Override
    public TextureEntity encode(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        TextureEntity texture = new TextureEntity();
        texture.setCompress(CompressModes.NONE);
        texture.setHeight(height);
        texture.setWidth(width);
        byte[] textureBytes = new byte[height * width * 4];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = bufferedImage.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                int red = (argb >> 16) & 0xFF;
                int green = (argb >> 8) & 0xFF;
                int blue = argb & 0xFF;
                textureBytes[(y * width + x) * 4] = (byte) blue;
                textureBytes[(y * width + x) * 4 + 1] = (byte) green;
                textureBytes[(y * width + x) * 4 + 2] = (byte) red;
                textureBytes[(y * width + x) * 4 + 3] = (byte) alpha;
            }
        }
        texture.setTextureData(textureBytes);
        return texture;
    }
}
