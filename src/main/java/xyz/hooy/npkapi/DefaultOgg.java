package xyz.hooy.npkapi;

public class DefaultOgg implements Ogg {

    private String name;

    private byte[] rawData;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public byte[] getRawData() {
        return rawData;
    }

    @Override
    public void setRawData(byte[] rawData) {
        this.rawData = rawData;
    }
}
