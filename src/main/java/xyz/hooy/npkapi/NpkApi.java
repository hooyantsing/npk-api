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
        NpkCoder npkCoder = new NpkCoder();
        register(npkCoder.suffix(), npkCoder);
        spiProvider();
    }

    private static void spiProvider() {
        ServiceLoader<AlbumCoder> albumCoders = ServiceLoader.load(AlbumCoder.class);
        ServiceLoader<SpriteCoder> spriteCoders = ServiceLoader.load(SpriteCoder.class);
        for (Coder albumCoder : albumCoders) {
            register(albumCoder.suffix(), albumCoder);
        }
        for (Coder spriteCoder : spriteCoders) {
            register(spriteCoder.suffix(), spriteCoder);
        }
    }

    public static void register(String suffix, Coder coder) {
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

    public static void save(String savePath, List<Album> albums, String format) throws IOException {
        checkAndCreateDirectories(savePath);
        saveAlbums(savePath, albums, format);
    }

    private static List<Album> loadAlbums(List<String> filePaths) throws IOException {
        List<Album> albums = new ArrayList<>();
        Map<String, Album> thirdCoderPathEntityMap = new LinkedHashMap<>();
        for (String filePath : filePaths) {
            String suffix = getFileSuffix(filePath);
            Coder coder = coderMap.get(suffix);
            if (Objects.nonNull(coder)) {
                if (coder instanceof NpkCoder) {
                    NpkCoder npkCoder = (NpkCoder) coder;
                    List<Album> entities = npkCoder.load(filePath);
                    albums.addAll(entities);
                } else if (coder instanceof AlbumCoder) {
                    AlbumCoder albumCoder = (AlbumCoder) coder;
                    Album album = albumCoder.load(filePath);
                    String albumSuffix = albumCoder.support() == AlbumSuffixModes.IMAGE ? "img" : "ogg";
                    album.setPath(generateAlbumPath(filePath, albumSuffix));
                    albums.add(album);
                } else if (coder instanceof SpriteCoder) {
                    SpriteCoder spriteCoder = (SpriteCoder) coder;
                    Sprite sprite = spriteCoder.load(filePath);
                    String albumPath = generateAlbumPath(filePath, "img");
                    Album album = thirdCoderPathEntityMap.get(albumPath);
                    if (Objects.isNull(album)) {
                        Album newAlbum = new Album(sprite.getPicture());
                        newAlbum.setPath(albumPath);
                        thirdCoderPathEntityMap.put(albumPath, newAlbum);
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

    private static void saveAlbums(String savePath, List<Album> albums, String format) throws IOException {
        format = format.toLowerCase();
        Coder coder = coderMap.get(format);
        if (Objects.nonNull(coder)) {
            if (coder instanceof NpkCoder) {
                NpkCoder npkCoder = ((NpkCoder) coder);
                String savedPath = Paths.get(savePath, generateFileName(UUID.randomUUID().toString(), npkCoder.suffix())).toString();
                npkCoder.save(savedPath, albums);
            } else if (coder instanceof AlbumCoder) {
                AlbumCoder albumCoder = ((AlbumCoder) coder);
                for (Album album : albums) {
                    if (albumCoder.support() == album.getAlbumSuffixMode()) {
                        String savedPath = Paths.get(savePath, generateFileName(album.getPath(), albumCoder.suffix())).toString();
                        albumCoder.save(savedPath, album);
                    }
                }
            } else if (coder instanceof SpriteCoder) {
                SpriteCoder spriteCoder = ((SpriteCoder) coder);
                for (Album album : albums) {
                    for (Sprite sprite : album.getSprites()) {
                        String savedPath = Paths.get(savePath, generateFileName(sprite.getParent().getPath(), spriteCoder.suffix())).toString();
                        spriteCoder.save(savedPath, sprite);
                    }
                }
            }
        } else {
            throw new UnsupportedEncodingException("Not found " + format + " coder.");
        }
    }

    private static void checkAndCreateDirectories(String savePath) throws IOException {
        Path path = Paths.get(savePath);
        if (!Files.isDirectory(path)) {
            Files.createDirectories(path);
        }
    }

    private static String generateAlbumPath(String filePath, String suffix) {
        String fileName = Paths.get(filePath).getFileName().toString();
        int endIndex = fileName.lastIndexOf('.');
        if (endIndex != -1) {
            fileName = fileName.substring(0, endIndex);
        }
        return fileName.replace('-', '/') + "." + suffix;
    }

    private static String generateFileName(String albumPath, String suffix) {
        int endIndex = albumPath.lastIndexOf('.');
        if (endIndex != -1) {
            albumPath = albumPath.substring(0, endIndex);
        }
        return albumPath.replace('/', '-') + "." + suffix;
    }

    private static String getFileSuffix(String fileName) {
        int endIndex = fileName.lastIndexOf('.');
        if (endIndex != -1) {
            fileName = fileName.substring(endIndex + 1);
        }
        return fileName.toLowerCase();
    }
}
