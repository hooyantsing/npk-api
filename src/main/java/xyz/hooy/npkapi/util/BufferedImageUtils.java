package xyz.hooy.npkapi.util;

import xyz.hooy.npkapi.component.IIOMetadataExpansion;
import xyz.hooy.npkapi.component.MemoryStream;
import xyz.hooy.npkapi.constant.ColorLinkTypes;
import xyz.hooy.npkapi.constant.SupportedImages;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

public final class BufferedImageUtils {

    private BufferedImageUtils() {
    }

    public static byte[] toArray(BufferedImage bufferedImage, ColorLinkTypes type) {
        byte[] data = toArray(bufferedImage);
        MemoryStream stream = new MemoryStream(data.length);
        for (int i = 0; i < data.length; i += 4) {
            byte[] temp = new byte[4];
            System.arraycopy(data, i, temp, 0, temp.length);
            ColorLinkUtils.writeColor(stream, temp, type);
        }
        return stream.toArray();
    }

    public static byte[] toArray(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        MemoryStream stream = new MemoryStream(width * height * 4);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = bufferedImage.getRGB(x, y);
                stream.writeInt(argb);
            }
        }
        return stream.toArray();
    }

    public static BufferedImage fromArray(byte[] data, int width, int height, ColorLinkTypes type) {
        MemoryStream stream = new MemoryStream(data.length);
        stream.write(data);
        byte[] bufferedImageData = new byte[width * height * 4];
        for (int i = 0; i < bufferedImageData.length; i += 4) {
            ColorLinkUtils.readColor(stream, type, bufferedImageData, i);
        }
        return fromArray(bufferedImageData, width, height);
    }

    public static BufferedImage fromArray(byte[] data, int width, int height) {
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        MemoryStream stream = new MemoryStream(data.length);
        stream.write(data);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = stream.readInt();
                bufferedImage.setRGB(x, y, argb);
            }
        }
        return bufferedImage;
    }

    public static List<BufferedImage> readImage(String imagePath) throws IOException {
        ImageReader reader = null;
        try (ImageInputStream in = ImageIO.createImageInputStream(new File(imagePath))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new IOException("No suitable reader found for image file!");
            }
            reader = readers.next();
            reader.setInput(in);
            int framesLength = reader.getNumImages(true);
            List<BufferedImage> bufferedImages = new ArrayList<>(framesLength);
            for (int i = 0; i < framesLength; i++) {
                BufferedImage frame = reader.read(i);
                bufferedImages.add(frame);
            }
            return bufferedImages;
        } finally {
            if (Objects.nonNull(reader)) {
                reader.dispose();
            }
        }
    }

    public static void writeImage(String imagePath, BufferedImage bufferedImage, SupportedImages supportedImage) throws IOException {
        writeImage(imagePath, Collections.singletonList(bufferedImage), supportedImage);
    }

    public static void writeImage(String imagePath, List<BufferedImage> bufferedImages, SupportedImages supportedImage) throws IOException {
        File imageFile = new File(imagePath);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(supportedImage.name());
        if (!writers.hasNext()) {
            throw new IOException("No " + supportedImage + " writer found!");
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(imageFile)) {
            writer.setOutput(output);
            if (writer.canWriteSequence()) {
                ImageWriteParam defaultWriteParam = writer.getDefaultWriteParam();
                IIOMetadata defaultImageMetadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(bufferedImages.get(0)), defaultWriteParam);
                defaultImageMetadata = new IIOMetadataExpansion(defaultImageMetadata).setDisposalMethod(IIOMetadataExpansion.DisposalMethod.RESTORE_TO_BACKGROUND_COLOR).apply();
                writer.prepareWriteSequence(defaultImageMetadata);
                for (BufferedImage bufferedImage : bufferedImages) {
                    IIOImage iioImage = new IIOImage(bufferedImage, null, defaultImageMetadata);
                    writer.writeToSequence(iioImage, defaultWriteParam);
                }
                writer.endWriteSequence();
            } else {
                writer.write(bufferedImages.get(0));
            }
        } finally {
            if (Objects.nonNull(writer)) {
                writer.dispose();
            }
        }
    }
}
