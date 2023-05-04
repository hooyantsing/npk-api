package xyz.hooy.npk.api.util;

import xyz.hooy.npk.api.model.IndexConstant;
import xyz.hooy.npk.api.model.Texture;
import xyz.hooy.npk.api.model.TextureAttribute;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class TextureUtils {

    public static void toPng(Texture texture, String fileName) throws IOException {
        byte[] textureBytes = texture.getTexture();
        TextureAttribute attribute = texture.getTextureAttribute();
        int width = attribute.getWidth();
        int height = attribute.getHeight();
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR_PRE);
        byte[] tempBytes = new byte[4];

        if (attribute.getType() == IndexConstant.TYPE_TEXTURE_ARGB1555) {
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    tempBytes[0] = (byte) ((textureBytes[i * width * 2 + j * 2] & 0x003F) << 3); // blue
                    tempBytes[1] = (byte) ((((textureBytes[i * width * 2 + j * 2 + 1] & 0x0003) << 3) | ((textureBytes[i * width * 2 + j * 2] >> 5) & 0x0007)) << 3); // green
                    tempBytes[2] = (byte) (((textureBytes[i * width * 2 + j * 2 + 1] & 127) >> 2) << 3);   // red
                    tempBytes[3] = (byte) ((textureBytes[i * width * 2 + j * 2 + 1] >> 7) == 0 ? 0 : 255); // alpha
                    int data = ByteUtils.bytesToInt(tempBytes);
                    bufferedImage.setRGB(j, i, data);
                }
            }
        } else if (attribute.getType() == IndexConstant.TYPE_TEXTURE_ARGB4444) {
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    tempBytes[0] = (byte) ((textureBytes[i * width * 2 + j * 2 + 0] & 0x0F) << 4); // blue
                    tempBytes[1] = (byte) (((textureBytes[i * width * 2 + j * 2 + 0] & 0xF0) >> 4) << 4); // green
                    tempBytes[2] = (byte) ((textureBytes[i * width * 2 + j * 2 + 1] & 0x0F) << 4);   // red
                    tempBytes[3] = (byte) (((textureBytes[i * width * 2 + j * 2 + 1] & 0xF0) >> 4) << 4); // alpha
                    int data = ByteUtils.bytesToInt(tempBytes);
                    bufferedImage.setRGB(j, i, data);
                }
            }
        } else if (attribute.getType() == IndexConstant.TYPE_TEXTURE_ARGB8888) {
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    tempBytes[0] = textureBytes[i * width * 4 + j * 4 + 0]; // blue
                    tempBytes[1] = textureBytes[i * width * 4 + j * 4 + 1]; // green
                    tempBytes[2] = textureBytes[i * width * 4 + j * 4 + 2]; // red
                    tempBytes[3] = textureBytes[i * width * 4 + j * 4 + 3]; // alpha
                    int data = ByteUtils.bytesToInt(tempBytes);
                    bufferedImage.setRGB(j, i, data);
                }
            }
        } else {
            throw new UnsupportedOperationException(String.format("The current type is not supported %s", attribute.getType()));
        }
        ImageIO.write(bufferedImage, "png", new File(fileName));
    }
}
