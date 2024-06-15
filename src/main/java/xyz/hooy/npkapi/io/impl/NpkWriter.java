package xyz.hooy.npkapi.io.impl;

import xyz.hooy.npkapi.npk.NpkCore;
import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class NpkWriter {
   public void write(String path, List<Album> albums) throws IOException{
       try {
           NpkCore.save(path, albums);
       } catch (NoSuchAlgorithmException e) {
           throw new IOException(e);
       }
   }

    public String suffix() {
        return "npk";
    }
}
