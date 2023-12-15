package xyz.hooy.npk.api.entity;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
public class TextureEntity extends AbstractIndex {

    private final TextureAttribute textureAttribute = new TextureAttribute();
    private byte[] texture = new byte[0];

    public TextureAttribute getTextureAttribute() {
        return textureAttribute;
    }

    public byte[] getTexture() {
        return texture;
    }

    public void setTexture(byte[] texture) {
        this.texture = texture;
    }

    @Override
    public String toString() {
        return "Texture{" +
                "textureAttribute=" + textureAttribute +
                '}';
    }
}
