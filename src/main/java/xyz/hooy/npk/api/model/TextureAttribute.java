package xyz.hooy.npk.api.model;

public class TextureAttribute extends AbstractIndexAttribute {

    private Integer width;
    private Integer height;
    private Integer x;
    private Integer y;
    private Integer frameWidth;
    private Integer frameHeight;

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public Integer getFrameWidth() {
        return frameWidth;
    }

    public void setFrameWidth(Integer frameWidth) {
        this.frameWidth = frameWidth;
    }

    public Integer getFrameHeight() {
        return frameHeight;
    }

    public void setFrameHeight(Integer frameHeight) {
        this.frameHeight = frameHeight;
    }

    @Override
    public String toString() {
        return "TextureAttribute{" +
                "width=" + width +
                ", height=" + height +
                ", x=" + x +
                ", y=" + y +
                ", frameWidth=" + frameWidth +
                ", frameHeight=" + frameHeight +
                '}';
    }
}
