package xyz.hooy.npk.api.model;

public class Texture extends AbstractIndex {

    private final TextureAttribute textureAttribute = new TextureAttribute();
    private byte[] texture;

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
