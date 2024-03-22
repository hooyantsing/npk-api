package xyz.hooy.npkapi;

import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.coder.*;
import xyz.hooy.npkapi.entity.ImgEntity;
import xyz.hooy.npkapi.entity.TextureEntity;

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
        List<Coder> coders = Arrays.asList(new NpkCoder(), new GifCoder(), new PngCoder());
        for (Coder coder : coders) {
            register(coder.suffix(), coder);
        }
    }

    private static void register(String suffix, Coder coder) {
        coderMap.put(suffix, coder);
        log.info("Register coder: {}, support suffix file: {}.", coder.getClass().getName(), suffix);
    }

    public static List<ImgEntity> load(String loadPath) throws IOException {
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

    private static List<ImgEntity> loadImg(List<String> filePaths) throws IOException {
        List<ImgEntity> imgEntities = new ArrayList<>();
        Map<String, ImgEntity> thirdCoderPathEntityMap = new LinkedHashMap<>();
        for (String filePath : filePaths) {
            String suffix = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase();
            Coder coder = coderMap.get(suffix);
            if (Objects.nonNull(coder)) {
                if (coder instanceof NpkCoder) {
                    NpkCoder npkCoder = (NpkCoder) coder;
                    List<ImgEntity> entities = npkCoder.load(filePath);
                    imgEntities.addAll(entities);
                } else if (coder instanceof SecondCoder) {
                    SecondCoder secondCoder = (SecondCoder) coder;
                    ImgEntity img = secondCoder.load(filePath);
                    String imgPath = filePath.replace('_', '/') + ".img";
                    img.setPath(imgPath);
                    imgEntities.add(img);
                } else if (coder instanceof ThirdCoder) {
                    ThirdCoder thirdCoder = (ThirdCoder) coder;
                    TextureEntity texture = thirdCoder.load(filePath);
                    String imgPath = filePath.replace('_', '/').substring(0, filePath.lastIndexOf('-')) + ".img";
                    ImgEntity imgEntity = thirdCoderPathEntityMap.get(imgPath);
                    if (Objects.isNull(imgEntity)) {
                        ImgEntity newImgEntity = new ImgEntity(Collections.singletonList(texture.getPicture()));
                        newImgEntity.setPath(imgPath);
                        thirdCoderPathEntityMap.put(imgPath, newImgEntity);
                    } else {
                        imgEntity.addTextureEntities(texture);
                    }
                }
            } else {
                throw new UnsupportedEncodingException("Not found " + suffix + " coder, not loaded " + filePath + ".");
            }
        }
        imgEntities.addAll(thirdCoderPathEntityMap.values());
        return imgEntities;
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
                if (coder instanceof NpkCoder) {
                    NpkCoder npkCoder = ((NpkCoder) coder);
                    String fileName = UUID.randomUUID() + "." + npkCoder.suffix();
                    npkCoder.save(Paths.get(savePath, fileName).toString(), imgEntities);
                } else if (coder instanceof SecondCoder) {
                    SecondCoder secondCoder = ((SecondCoder) coder);
                    for (ImgEntity imgEntity : imgEntities) {
                        if (secondCoder.match(imgEntity.getImgType())) {
                            String pathName = imgEntity.getPath();
                            String imageName = pathName.substring(0, pathName.indexOf('.')).replace('/', '_') + "." + secondCoder.suffix();
                            secondCoder.save(Paths.get(savePath, imageName).toString(), imgEntity);
                        }
                    }
                } else if (coder instanceof ThirdCoder) {
                    ThirdCoder thirdCoder = ((ThirdCoder) coder);
                    for (ImgEntity imgEntity : imgEntities) {
                        for (TextureEntity textureEntity : imgEntity.getTextureEntities()) {
                            String pathName = textureEntity.getParent().getPath();
                            String imageName = pathName.substring(0, pathName.indexOf('.')).replace('/', '_') + "-" + textureEntity.getIndex() + "." + thirdCoder.suffix();
                            thirdCoder.save(Paths.get(savePath, imageName).toString(), textureEntity);
                        }
                    }
                }
            }
        }
    }
}
