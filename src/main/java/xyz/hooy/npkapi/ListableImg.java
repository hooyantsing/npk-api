package xyz.hooy.npkapi;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

public abstract class ListableImg implements Img {

    String name = "unknown";

    protected Access delegateImgAccess;

    List<Frame> frames = new ArrayList<>();
    int version = -1;

    @Override
    public void read(ImageInputStream stream) throws IOException {
        if (delegateImgAccess == null) {
            throw new IllegalStateException("Read unavailable.");
        }
        delegateImgAccess.read(stream);
    }

    @Override
    public void write(ImageOutputStream stream) throws IOException {
        if (delegateImgAccess == null) {
            throw new IllegalStateException("Write unavailable.");
        }
        delegateImgAccess.write(stream);
    }

    @Override
    public void addFrame(int index, int referenceIndex) {
        if (referenceIndex >= index) {
            throw new IllegalArgumentException("Backward references are not allowed.");
        }
        Frame frame = frames.get(referenceIndex);
        ReferenceFrame referenceFrame = new ReferenceFrame();
        referenceFrame.type = Frame.TYPE_REFERENCE;
        referenceFrame.reference = referenceIndex;
        referenceFrame.frame = frame;
        insertFrame(index, referenceFrame);
    }

    @Override
    public void addFrame(int index, int type, BufferedImage image) {
        supportedImageFrameTypeThrowException(type);
        ImageFrame imageFrame = new ImageFrame();
        imageFrame.type = type;
        imageFrame.compressed = ImageFrame.COMPRESSED;
        imageFrame.width = image.getWidth();
        imageFrame.height = image.getHeight();
        imageFrame.x = image.getMinX();
        imageFrame.y = image.getMinY();
        imageFrame.frameWidth = image.getWidth();
        imageFrame.frameHeight = image.getHeight();
        imageFrame.image = image;
        conventImageToData(imageFrame);
        insertFrame(index, imageFrame);
    }

    @Override
    public void addFrame(int index, int type, BufferedImage image, Rectangle[] rectangles) {
        for (Rectangle rectangle : rectangles) {
            BufferedImage subImage = image.getSubimage(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
            addFrame(index++, type, subImage);
        }
    }

    @Override
    public void removeFrame(int index) {
        ensureNoReferencesTo(index);
        deleteFrame(index);
    }

    @Override
    public int getFrameSize() {
        return frames.size();
    }

    @Override
    public BufferedImage getImage(int index) {
        Frame frame = frames.get(index);
        if (frame.isReference()) {
            ReferenceFrame referenceFrame = (ReferenceFrame) frame;
            return getImage(referenceFrame.reference);
        } else {
            ImageFrame imageFrame = (ImageFrame) frame;
            if (imageFrame.image == null) {
                conventDataToImage(imageFrame);
            }
            return imageFrame.image;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String imgName) {
        this.name = imgName;
    }

    @Override
    public int getVersion() {
        return version;
    }


    protected abstract void conventImageToData(ImageFrame imageFrame);

    protected abstract void conventDataToImage(ImageFrame imageFrame);

    private void ensureNoReferencesTo(int index) {
        for (int i = 0; i < frames.size(); i++) {
            Frame existedFrame = frames.get(i);
            if (existedFrame.isReference()) {
                ReferenceFrame referenceFrame = (ReferenceFrame) existedFrame;
                if (referenceFrame.reference == index) {
                    throw new IllegalArgumentException("Frame " + i + " is referencing Frame " + index);
                }
            }
        }
    }

    protected void supportedImageFrameTypeThrowException(int type) {
        // Arrow all
    }

    protected void insertFrame(int index, Frame frame) {
        incrementReferencesFrom(index);
        frames.add(index, frame);
    }

    protected void deleteFrame(int index) {
        decrementReferencesFrom(index);
        frames.remove(index);
    }

    private void incrementReferencesFrom(int index) {
        for (Frame existedFrame : frames) {
            if (existedFrame.isReference()) {
                ReferenceFrame referenceFrame = (ReferenceFrame) existedFrame;
                if (referenceFrame.reference >= index) {
                    referenceFrame.reference++;
                }
            }
        }
    }

    private void decrementReferencesFrom(int index) {
        for (Frame existedFrame : frames) {
            if (existedFrame.isReference()) {
                ReferenceFrame referenceFrame = (ReferenceFrame) existedFrame;
                if (referenceFrame.reference >= index) {
                    referenceFrame.reference--;
                }
            }
        }
    }

    protected byte[] decompress(byte[] bytes) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (InflaterOutputStream inflaterOutputStream = new InflaterOutputStream(outputStream)) {
            inflaterOutputStream.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outputStream.toByteArray();
    }

    protected byte[] compress(byte[] bytes) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(outputStream)) {
            deflaterOutputStream.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outputStream.toByteArray();
    }
}
