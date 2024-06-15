package xyz.hooy.npkapi.io.impl;

import xyz.hooy.npkapi.io.AlbumReader;
import xyz.hooy.npkapi.npk.entity.Album;
import xyz.hooy.npkapi.npk.util.BufferedImageUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class GifAlbumReader implements AlbumReader {
    @Override
    public Album read(String path) throws IOException {
        ImageReader reader = null;
        try (ImageInputStream in = ImageIO.createImageInputStream(new File(path))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new IOException("No GIF reader found.");
            }
            reader = readers.next();
            reader.setInput(in);
            int framesLength = reader.getNumImages(true);
            List<BufferedImage> bufferedImages = new ArrayList<>(framesLength);
            for (int i = 0; i < framesLength; i++) {
                BufferedImage frame = reader.read(i);
                bufferedImages.add(frame);
            }
            return new Album(bufferedImages);
        } finally {
            if (Objects.nonNull(reader)) {
                reader.dispose();
            }
        }
    }

    @Override
    public String suffix() {
        return "gif";
    }
}
