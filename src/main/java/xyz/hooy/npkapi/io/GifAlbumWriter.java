package xyz.hooy.npkapi.io;

import lombok.Getter;
import lombok.Setter;
import xyz.hooy.npkapi.component.GifMetadataExpansion;
import xyz.hooy.npkapi.npk.entity.Album;
import xyz.hooy.npkapi.npk.entity.Sprite;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@Setter
public class GifAlbumWriter extends AbstractAlbumWriter {

    private int delayTime = 20;

    public GifAlbumWriter(String path) {
        super(path);
    }

    @Override
    protected void writeSingleFile(Path path, Album album) throws IOException {
        List<BufferedImage> bufferedImages = album.getSprites().stream().map(Sprite::getPicture).collect(Collectors.toList());
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(suffix());
        if (!writers.hasNext()) {
            throw new IOException("No GIF writer found.");
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(path.toFile())) {
            writer.setOutput(output);
            ImageWriteParam defaultWriteParam = writer.getDefaultWriteParam();
            IIOMetadata defaultImageMetadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(bufferedImages.get(0)), defaultWriteParam);
            defaultImageMetadata = new GifMetadataExpansion(defaultImageMetadata)
                    .setDisposalMethod(GifMetadataExpansion.DisposalMethod.RESTORE_TO_BACKGROUND_COLOR)
                    .setDelayTime(delayTime)
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
