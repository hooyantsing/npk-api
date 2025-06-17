package xyz.hooy.npkapi.impl;

import xyz.hooy.npkapi.Access;
import xyz.hooy.npkapi.Img;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class ListableImg implements Img {

    String name = "Unknown";

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
    public void setFrame(int index, int referenceIndex) {
        if (referenceIndex >= index) {
            throw new IllegalArgumentException("Backward references are not allowed.");
        }
        Frame frame = frames.get(referenceIndex);
        ReferenceFrame referenceFrame = new ReferenceFrame();
        referenceFrame.reference = referenceIndex;
        referenceFrame.frame = frame;
        insertFrame(index, referenceFrame);
    }

    @Override
    public void setFrame(int index, int type, BufferedImage image) {
        supportedImageFrameTypeThrowException(type);
        ImageFrame imageFrame = new ImageFrame();
        imageFrame.type = type;
        imageFrame.image = image;
        imageFrame.compressed = ImageFrame.COMPRESSED;
        imageFrame.width = image.getWidth();
        imageFrame.height = image.getHeight();
        imageFrame.x = image.getMinX();
        imageFrame.y = image.getMinY();
        imageFrame.frameWidth = image.getWidth();
        imageFrame.frameHeight = image.getHeight();
        conventImageToData(imageFrame);
        insertFrame(index, imageFrame);
    }

    @Override
    public void setFrame(int index, int type, BufferedImage image, Rectangle[] rectangles) {
        for (Rectangle rectangle : rectangles) {
            BufferedImage subImage = image.getSubimage(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
            setFrame(index++, type, subImage);
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
}
