package xyz.hooy.npkapi.coder;

import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.component.BufferedAudio;
import xyz.hooy.npkapi.constant.AlbumSuffixModes;
import xyz.hooy.npkapi.entity.Album;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
public class OggAlbumCoder implements AlbumCoder{
    @Override
    public Album load(String loadPath) throws IOException {
        byte[] audioData = Files.readAllBytes(Paths.get(loadPath));
        BufferedAudio bufferedAudio = new BufferedAudio(loadPath, audioData);
        Album album = new Album(bufferedAudio);
        log.info("Loaded file: {}.", loadPath);
        return album;
    }

    @Override
    public void save(String savePath, Album album) throws IOException {
        byte[] audioData = album.getData();
        Files.write(Paths.get(savePath),audioData);
        log.info("Saved file: {}.", savePath);
    }

    @Override
    public AlbumSuffixModes support() {
        return AlbumSuffixModes.AUDIO;
    }

    @Override
    public String suffix() {
        return "ogg";
    }
}
