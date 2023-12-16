package xyz.hooy.npk.api.color;

import xyz.hooy.npk.api.constant.CompressModes;
import xyz.hooy.npk.api.entity.TextureEntity;
import xyz.hooy.npk.api.util.ByteUtils;

import java.awt.image.BufferedImage;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
class Argb1555Converter extends AbstractColorConverter {

    @Override
    public BufferedImage decode(TextureEntity texture) {
        int width = texture.getWidth();
        int height = texture.getHeight();
        byte[] textureData = texture.getTextureData();
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR_PRE);
        byte[] tempBytes = new byte[4];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                tempBytes[0] = (byte) ((textureData[i * width * 2 + j * 2] & 0x003F) << 3); // blue
                tempBytes[1] = (byte) ((((textureData[i * width * 2 + j * 2 + 1] & 0x0003) << 3) | ((textureData[i * width * 2 + j * 2] >> 5) & 0x0007)) << 3); // green
                tempBytes[2] = (byte) (((textureData[i * width * 2 + j * 2 + 1] & 127) >> 2) << 3);   // red
                tempBytes[3] = (byte) ((textureData[i * width * 2 + j * 2 + 1] >> 7) == 0 ? 0 : 255); // alpha
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
        byte[] textureBytes = new byte[height * width * 2];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = bufferedImage.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                int red = (argb >> 16) & 0xFF;
                int green = (argb >> 8) & 0xFF;
                int blue = argb & 0xFF;
                int argb1555 = ((alpha >> 7) << 15) | ((red >> 3) << 10) | ((green >> 3) << 5) | (blue >> 3);
                textureBytes[(y * width + x) * 2] = (byte) (argb1555 & 0xFF);
                textureBytes[(y * width + x) * 2 + 1] = (byte) ((argb1555 >> 8) & 0xFF);
            }
        }
        texture.setTextureData(textureBytes);
        return texture;
    }
}
