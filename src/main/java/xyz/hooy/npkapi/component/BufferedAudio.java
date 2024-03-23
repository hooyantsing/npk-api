package xyz.hooy.npkapi.component;

import lombok.Data;

@Data
public class BufferedAudio {

    private String path;
    private Integer length;
    private byte[] data;

    public BufferedAudio(String path, byte[] data) {
        this.path = path;
        this.data = data;
        this.length = data.length;
    }
}
