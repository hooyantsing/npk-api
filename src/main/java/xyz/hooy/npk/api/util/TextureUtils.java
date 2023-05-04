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
        BufferedImage bufferedImage;
        TextureAttribute attribute = texture.getTextureAttribute();
        if (attribute.getType() == IndexConstant.TYPE_TEXTURE_ARGB1555) {
            bufferedImage = new BufferedImage(attribute.getWidth(), attribute.getHeight(), BufferedImage.TYPE_4BYTE_ABGR_PRE);
            byte[] tempBytes = new byte[4];
            for (int i = 0; i < attribute.getHeight(); i++) {
                for (int j = 0; j < attribute.getWidth(); j++) {
                    tempBytes[0] = (byte) ((texture.getTexture()[i * attribute.getWidth() * 2 + j * 2] & 0x003F) << 3); // blue
                    tempBytes[1] = (byte) ((((texture.getTexture()[i * attribute.getWidth() * 2 + j * 2 + 1] & 0x0003) << 3) | ((texture.getTexture()[i * attribute.getWidth() * 2 + j * 2] >> 5) & 0x0007)) << 3); // green
                    tempBytes[2] = (byte) (((texture.getTexture()[i * attribute.getWidth() * 2 + j * 2 + 1] & 127) >> 2) << 3);   // red
                    tempBytes[3] = (byte) ((texture.getTexture()[i * attribute.getWidth() * 2 + j * 2 + 1] >> 7) == 0 ? 0 : 255); // alpha
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
