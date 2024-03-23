package xyz.hooy.npkapi;

import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.coder.*;
import xyz.hooy.npkapi.constant.AlbumSuffixModes;
import xyz.hooy.npkapi.entity.Album;
import xyz.hooy.npkapi.entity.Sprite;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class NpkApi {

    private static final Map<String, Coder> coderMap = new HashMap<>();

    static {
        List<Coder> coders = Arrays.asList(new NpkCoder(), new GifAlbumCoder(), new PngSpriteCoder(), new JpgSpriteCoder(), new OggAlbumCoder());
        for (Coder coder : coders) {
            register(coder.suffix(), coder);
        }
    }

    private static void register(String suffix, Coder coder) {
        coderMap.put(suffix, coder);
        log.info("Register coder: {}, support suffix file: {}.", coder.getClass().getName(), suffix);
    }

    public static List<Album> load(String loadPath) throws IOException {
        Path path = Paths.get(loadPath);
        if (Files.isRegularFile(path)) {
            return loadAlbums(Collections.singletonList(loadPath));
        } else if (Files.isDirectory(path)) {
            try (Stream<Path> walk = Files.walk(path)) {
                List<String> filePaths = walk.filter(Files::isRegularFile).map(Path::toString).sorted().collect(Collectors.toList());
                return loadAlbums(filePaths);
            }
        } else {
            throw new IllegalArgumentException(loadPath + ", it's not a file or a directory.");
        }
    }

    private static List<Album> loadAlbums(List<String> filePaths) throws IOException {
        List<Album> albums = new ArrayList<>();
        Map<String, Album> thirdCoderPathEntityMap = new LinkedHashMap<>();
        for (String filePath : filePaths) {
            String suffix = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase();
            Coder coder = coderMap.get(suffix);
            if (Objects.nonNull(coder)) {
                if (coder instanceof NpkCoder) {
                    NpkCoder npkCoder = (NpkCoder) coder;
                    List<Album> entities = npkCoder.load(filePath);
                    albums.addAll(entities);
                } else if (coder instanceof AlbumCoder) {
                    AlbumCoder albumCoder = (AlbumCoder) coder;
                    Album album = albumCoder.load(filePath);
                    String albumSuffix = albumCoder.support() == AlbumSuffixModes.IMAGE ? ".img" : ".ogg";
                    String fileName = Paths.get(filePath).getFileName().toString();
                    String pathName = fileName.substring(0, fileName.lastIndexOf('.')).replace('-', '/') + albumSuffix;
                    album.setPath(pathName);
                    albums.add(album);
                } else if (coder instanceof SpriteCoder) {
                    SpriteCoder spriteCoder = (SpriteCoder) coder;
                    Sprite sprite = spriteCoder.load(filePath);
                    String fileName = Paths.get(filePath).getFileName().toString();
                    String pathName = fileName.substring(0, fileName.lastIndexOf('-')).replace('-', '/') + ".img";
                    Album album = thirdCoderPathEntityMap.get(pathName);
                    if (Objects.isNull(album)) {
                        Album newAlbum = new Album(Collections.singletonList(sprite.getPicture()));
                        newAlbum.setPath(pathName);
                        thirdCoderPathEntityMap.put(pathName, newAlbum);
                    } else {
                        album.addSprite(sprite);
                    }
                }
            } else {
                throw new UnsupportedEncodingException("Not found " + suffix + " coder, not loaded " + filePath + ".");
            }
        }
        albums.addAll(thirdCoderPathEntityMap.values());
        return albums;
    }

    public static void save(String savePath, List<Album> albums, String format) throws IOException {
        Path path = Paths.get(savePath);
        if (!Files.isDirectory(path)) {
            Files.createDirectories(path);
        }
        format = format.toLowerCase();
        Coder coder = coderMap.get(format);
        if (Objects.nonNull(coder)) {
            if (coder instanceof NpkCoder) {
                NpkCoder npkCoder = ((NpkCoder) coder);
                String fileName = UUID.randomUUID() + "." + npkCoder.suffix();
                npkCoder.save(Paths.get(savePath, fileName).toString(), albums);
            } else if (coder instanceof AlbumCoder) {
                AlbumCoder albumCoder = ((AlbumCoder) coder);
                for (Album album : albums) {
                    if (albumCoder.support() == album.getAlbumSuffixMode()) {
                        String pathName = album.getPath();
                        String filePath = pathName.substring(0, pathName.lastIndexOf('.')).replace('/', '-') + "." + albumCoder.suffix();
                        albumCoder.save(Paths.get(savePath, filePath).toString(), album);
                    }
                }
            } else if (coder instanceof SpriteCoder) {
                SpriteCoder spriteCoder = ((SpriteCoder) coder);
                for (Album album : albums) {
                    for (Sprite sprite : album.getSprites()) {
                        String pathName = sprite.getParent().getPath();
                        String filePath = pathName.substring(0, pathName.lastIndexOf('.')).replace('/', '-') + "-" + sprite.getIndex() + "." + spriteCoder.suffix();
                        spriteCoder.save(Paths.get(savePath, filePath).toString(), sprite);
                    }
                }
            }
        } else {
            throw new UnsupportedEncodingException("Not found " + format + " coder.");
        }
    }
}
