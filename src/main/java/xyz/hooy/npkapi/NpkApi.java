package xyz.hooy.npkapi;

import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.coder.*;
import xyz.hooy.npkapi.entity.ImgEntity;

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
        List<Coder> coders = Arrays.asList(new NpkCoder(), new GifCoder(), new PngCoder(), new JpgCoder());
        for (Coder coder : coders) {
            register(coder.getSuffix(), coder);
        }
    }

    private static void register(String suffix, Coder coder) {
        coderMap.put(suffix, coder);
        log.info("Register coder: {}, support suffix file: {}.", coder.getClass().getName(), suffix);
    }

    public static List<ImgEntity> load(String loadPath) throws IOException {
        Path path = Paths.get(loadPath);
        Map<String, List<String>> suffixPaths = new LinkedHashMap<>();
        if (Files.isDirectory(path)) {
            try (Stream<Path> walk = Files.walk(path)) {
                List<String> filePaths = walk.filter(Files::isRegularFile).map(Path::toString).collect(Collectors.toList());
                for (String filePath : filePaths) {
                    String suffix = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase();
                    List<String> suffixPath = suffixPaths.computeIfAbsent(suffix, s -> new ArrayList<>());
                    suffixPath.add(filePath);
                }
                List<ImgEntity> imgEntities = new ArrayList<>();
                for (Map.Entry<String, List<String>> suffixFile : suffixPaths.entrySet()) {
                    String suffix = suffixFile.getKey();
                    List<String> suffixPath = suffixFile.getValue();
                    Coder coder = coderMap.get(suffix);
                    if (Objects.nonNull(coder)) {
                        List<ImgEntity> loadImgEntities = coder.load(suffixPath);
                        imgEntities.addAll(loadImgEntities);
                    } else {
                        throw new UnsupportedEncodingException("Not found " + suffix + " coder, not loaded " + loadPath +".");
                    }
                }
                return imgEntities;
            }
        } else if (Files.isRegularFile(path)) {
            String suffix = loadPath.substring(loadPath.lastIndexOf('.') + 1).toLowerCase();
            Coder coder = coderMap.get(suffix);
            if (Objects.nonNull(coder)) {
                List<String> suffixPath = Collections.singletonList(loadPath);
                return coder.load(suffixPath);
            } else {
                throw new UnsupportedEncodingException("Not found " + suffix + " coder, not loaded " + loadPath+".");
            }
        } else {
            throw new IllegalArgumentException(loadPath + ", it's not a file or a directory.");
        }
    }

    public static void save(String savePath, List<ImgEntity> imgEntities, String format) throws IOException {
        Path path = Paths.get(savePath);
        if (Files.isRegularFile(path)) {
            throw new IllegalArgumentException("The save path must be a directory.");
        } else if (Files.isDirectory(path)) {
            format = format.toLowerCase();
            Coder coder = coderMap.get(format);
            if (Objects.isNull(coder)) {
                throw new UnsupportedEncodingException("Not found " + format + " coder.");
            } else {
                coder.save(savePath, imgEntities);
            }
        }
    }
}
