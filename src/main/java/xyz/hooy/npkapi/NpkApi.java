package xyz.hooy.npkapi;

import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.coder.*;
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
        List<Coder> coders = Arrays.asList(new NpkCoder(), new GifAlbumCoder(), new PngSpriteCoder());
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
            return loadImg(Collections.singletonList(loadPath));
        } else if (Files.isDirectory(path)) {
            try (Stream<Path> walk = Files.walk(path)) {
                List<String> filePaths = walk.filter(Files::isRegularFile).map(Path::toString).sorted().collect(Collectors.toList());
                return loadImg(filePaths);
            }
        } else {
            throw new IllegalArgumentException(loadPath + ", it's not a file or a directory.");
        }
    }

    private static List<Album> loadImg(List<String> filePaths) throws IOException {
        List<Album> imgEntities = new ArrayList<>();
        Map<String, Album> thirdCoderPathEntityMap = new LinkedHashMap<>();
        for (String filePath : filePaths) {
            String suffix = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase();
            Coder coder = coderMap.get(suffix);
            if (Objects.nonNull(coder)) {
                if (coder instanceof NpkCoder) {
                    NpkCoder npkCoder = (NpkCoder) coder;
                    List<Album> entities = npkCoder.load(filePath);
                    imgEntities.addAll(entities);
                } else if (coder instanceof AlbumCoder) {
                    AlbumCoder albumCoder = (AlbumCoder) coder;
                    Album img = albumCoder.load(filePath);
                    String imgPath = filePath.replace('_', '/') + ".img";
                    img.setPath(imgPath);
                    imgEntities.add(img);
                } else if (coder instanceof SpriteCoder) {
                    SpriteCoder spriteCoder = (SpriteCoder) coder;
                    Sprite texture = spriteCoder.load(filePath);
                    String imgPath = filePath.replace('_', '/').substring(0, filePath.lastIndexOf('-')) + ".img";
                    Album album = thirdCoderPathEntityMap.get(imgPath);
                    if (Objects.isNull(album)) {
                        Album newAlbum = new Album(Collections.singletonList(texture.getPicture()));
                        newAlbum.setPath(imgPath);
                        thirdCoderPathEntityMap.put(imgPath, newAlbum);
                    } else {
                        album.addSprite(texture);
                    }
                }
            } else {
                throw new UnsupportedEncodingException("Not found " + suffix + " coder, not loaded " + filePath + ".");
            }
        }
        imgEntities.addAll(thirdCoderPathEntityMap.values());
        return imgEntities;
    }

    public static void save(String savePath, List<Album> imgEntities, String format) throws IOException {
        Path path = Paths.get(savePath);
        if (Files.isRegularFile(path)) {
            throw new IllegalArgumentException("The save path must be a directory.");
        } else if (Files.isDirectory(path)) {
            format = format.toLowerCase();
            Coder coder = coderMap.get(format);
            if (Objects.isNull(coder)) {
                throw new UnsupportedEncodingException("Not found " + format + " coder.");
            } else {
                if (coder instanceof NpkCoder) {
                    NpkCoder npkCoder = ((NpkCoder) coder);
                    String fileName = UUID.randomUUID() + "." + npkCoder.suffix();
                    npkCoder.save(Paths.get(savePath, fileName).toString(), imgEntities);
                } else if (coder instanceof AlbumCoder) {
                    AlbumCoder albumCoder = ((AlbumCoder) coder);
                    for (Album album : imgEntities) {
                        if (albumCoder.match(album.getAlbumSuffixMode())) {
                            String pathName = album.getPath();
                            String imageName = pathName.substring(0, pathName.indexOf('.')).replace('/', '_') + "." + albumCoder.suffix();
                            albumCoder.save(Paths.get(savePath, imageName).toString(), album);
                        }
                    }
                } else if (coder instanceof SpriteCoder) {
                    SpriteCoder spriteCoder = ((SpriteCoder) coder);
                    for (Album album : imgEntities) {
                        for (Sprite sprite : album.getSprites()) {
                            String pathName = sprite.getParent().getPath();
                            String imageName = pathName.substring(0, pathName.indexOf('.')).replace('/', '_') + "-" + sprite.getIndex() + "." + spriteCoder.suffix();
                            spriteCoder.save(Paths.get(savePath, imageName).toString(), sprite);
                        }
                    }
                }
            }
        }
    }
}
