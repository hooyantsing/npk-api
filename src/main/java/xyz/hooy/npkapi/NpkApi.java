package xyz.hooy.npkapi;

import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.coder.Coder;
import xyz.hooy.npkapi.coder.GifCoder;
import xyz.hooy.npkapi.coder.PngCoder;
import xyz.hooy.npkapi.entity.ImgEntity;

import java.io.IOException;
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
        List<Coder> coders = Arrays.asList(new GifCoder(), new PngCoder());
        for (Coder coder : coders) {
            register(coder.getSuffix(), coder);
        }
    }

    private static void register(String suffix, Coder coder) {
        coderMap.put(suffix, coder);
        log.info("Register Coder: {}, support suffix file: {}", coder.getClass().getName(), suffix);
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
                for (Map.Entry<String, List<String>> suffixFile : suffixPaths.entrySet()) {
                    String suffix = suffixFile.getKey();
                    List<String> suffixPath = suffixFile.getValue();
                    Coder coder = coderMap.get(suffix);
                    if (Objects.nonNull(coder)) {
                        return coder.load(suffixPath);
                    } else {
                        log.warn("Not found {} coder, not loaded {}", suffix, suffixPath.toString());
                    }
                }
            }
        } else if (Files.isRegularFile(path)) {
            String suffix = loadPath.substring(loadPath.lastIndexOf('.') + 1).toLowerCase();
            Coder coder = coderMap.get(suffix);
            if (Objects.nonNull(coder)) {
                List<String> suffixPath = Collections.singletonList(loadPath);
                return coder.load(suffixPath);
            } else {
                log.warn("Not found {} coder, not loaded {}", suffix, loadPath);
            }
        } else {
            log.warn("{}, it's not a file or a directory", loadPath);
        }
        return Collections.emptyList();
    }

    public static void save(String savePath, List<ImgEntity> imgEntities, String format) {

    }
}
