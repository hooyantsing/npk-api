package xyz.hooy.npk.codec;

import java.awt.image.BufferedImage;
import java.util.List;

public class Texture {

    private final List<BufferedImage> images;

    public Texture(List<BufferedImage> images) {
        this.images = images;
    }

    public List<BufferedImage> getImages() {
        return images;
    }
}
