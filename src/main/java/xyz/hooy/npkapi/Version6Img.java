package xyz.hooy.npkapi;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Version6Img extends Version4Img {

    public final static int VERSION = 6;

    List<Palette> palettes = new ArrayList<>(1);

    public Version6Img() {
        this.version = VERSION;
        this.delegateImgAccess = new Version6ImgAccess(this);
        palettes.add(palette);
    }

    @Override
    public void addFrame(int index, BufferedImage image) {
        multiplePalettesThrowException();
        super.addFrame(index, image);
    }

    @Override
    public void addFrame(int index, BufferedImage image, Rectangle[] rectangles) {
        multiplePalettesThrowException();
        super.addFrame(index, image, rectangles);
    }

    @Override
    public void addFrame(int index, int type, BufferedImage image) {
        multiplePalettesThrowException();
        super.addFrame(index, type, image);
    }

    @Override
    public void addFrame(int index, int referenceIndex) {
        multiplePalettesThrowException();
        super.addFrame(index, referenceIndex);
    }

    @Override
    public void addFrame(int index, int type, BufferedImage image, Rectangle[] rectangles) {
        multiplePalettesThrowException();
        super.addFrame(index, type, image, rectangles);
    }

    @Override
    protected void supportedImageFrameTypeThrowException(int type) {
        if (!Frame.isIndexed(type)) {
            throw new IllegalArgumentException("Img(v6) type must be INDEXED.");
        }
    }

    private void multiplePalettesThrowException() {
        if (palettes.size() > 1) {
            throw new IllegalStateException("Single image insertion not supported with multiple palettes.");
        }
    }

    public int getPaletteSize() {
        return palettes.size();
    }

    public int getActivePalette() {
        return palettes.indexOf(palette);
    }

    public void setActivePalette(int active) {
        if (active < 0 || active > palettes.size()) {
            throw new IllegalArgumentException("Index exceeds available palette range.");
        }
        this.palette = palettes.get(active);
    }
}
