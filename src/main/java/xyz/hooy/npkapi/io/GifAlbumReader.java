package xyz.hooy.npkapi.io;

import xyz.hooy.npkapi.npk.entity.Album;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class GifAlbumReader extends AbstractAlbumReader {

    public GifAlbumReader(String path) {
        super(path);
    }

    @Override
    protected Album read(Path singleFile) throws IOException {
        ImageReader reader = null;
        try (ImageInputStream in = ImageIO.createImageInputStream(path.toFile())) {
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
