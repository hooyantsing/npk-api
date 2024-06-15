package xyz.hooy.npkapi.io.impl;

import xyz.hooy.npkapi.component.GIFMetadataExpansion;
import xyz.hooy.npkapi.io.AlbumWriter;
import xyz.hooy.npkapi.npk.entity.Album;
import xyz.hooy.npkapi.npk.entity.Sprite;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GifAlbumWriter implements AlbumWriter {
    @Override
    public void write(String path, Album album) throws IOException {
        List<BufferedImage> bufferedImages = album.getSprites().stream().map(Sprite::getPicture).collect(Collectors.toList());
        File imageFile = new File(path);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(suffix());
        if (!writers.hasNext()) {
            throw new IOException("No GIF writer found.");
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(imageFile)) {
            writer.setOutput(output);
            ImageWriteParam defaultWriteParam = writer.getDefaultWriteParam();
            IIOMetadata defaultImageMetadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(bufferedImages.get(0)), defaultWriteParam);
            defaultImageMetadata = new GIFMetadataExpansion(defaultImageMetadata)
                    .setDisposalMethod(GIFMetadataExpansion.DisposalMethod.RESTORE_TO_BACKGROUND_COLOR)
                    .apply();
            writer.prepareWriteSequence(defaultImageMetadata);
            for (BufferedImage bufferedImage : bufferedImages) {
                IIOImage iioImage = new IIOImage(bufferedImage, null, defaultImageMetadata);
                writer.writeToSequence(iioImage, defaultWriteParam);
            }
            writer.endWriteSequence();
        } finally {
            if (Objects.nonNull(writer)) {
                writer.dispose();
            }
        }
    }

    @Override
    public String suffix() {
        return "gif";
    }
}
