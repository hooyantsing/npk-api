package xyz.hooy.npkapi.npk.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Pack {

    private List<Album> albums;

    public Pack() {
        this.albums = new ArrayList<>();
    }

    public Pack(List<Album> albums) {
        this.albums = albums;
    }
}
