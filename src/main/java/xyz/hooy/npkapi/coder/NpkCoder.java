package xyz.hooy.npkapi.coder;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import xyz.hooy.npkapi.component.MemoryStream;
import xyz.hooy.npkapi.constant.ImgVersions;
import xyz.hooy.npkapi.entity.ImgEntity;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class NpkCoder {

    public static final String NPK_FlAG = "NeoplePack_Bill";

    public static final String IMG_FLAG = "Neople Img File";

    public static final String IMAGE_FLAG = "Neople Image File";

    public static final String IMAGE_DIR = "ImagePacks2";

    public static final String SOUND_DIR = "SoundPacks";

    public static final String KEY_HEADER = "puchikon@neople dungeon and fighter ";

    public static byte[] Key = ("puchikon@neople dungeon and fighter " +
            "DNFDNFDNFDNFDNFDNFDNFDNFDNFDNF" +
            "DNFDNFDNFDNFDNFDNFDNFDNFDNFDNF" +
            "DNFDNFDNFDNFDNFDNFDNFDNFDNFDNF" +
            "DNFDNFDNFDNFDNFDNFDNFDNFDNFDNF" +
            "DNFDNFDNFDNFDNFDNFDNFDNFDNFDNF" +
            "DNFDNFDNFDNFDNFDNFDNFDNFDNFDNF" +
            "DNFDNFDNFDNFDNFDNFDNFDNFDNFDNF" +
            "DNFDNFDNF\0").getBytes(StandardCharsets.UTF_8);

    public static List<ImgEntity> readNpk(MemoryStream stream) {
        return readNpk(stream, null);
    }

    public static List<ImgEntity> readNpk(MemoryStream stream, String file) {
        List<ImgEntity> imgEntities = new ArrayList<>();
        String flag = stream.readString();
        if (StringUtils.equals(NPK_FlAG, flag)) {
            // 当文件是NPK时
            stream.seek(0, MemoryStream.SeekOrigin.Begin);
            imgEntities.addAll(readInfo(stream));
            if (!imgEntities.isEmpty()) {
                stream.seek(32, MemoryStream.SeekOrigin.Begin);
            }
        } else {
            ImgEntity imgEntity = new ImgEntity();
            if (Objects.nonNull(file)) {
                String suffix = StringUtils.substringAfterLast(file, ".");
                imgEntity.setPath(suffix);
            }
            imgEntities.add(imgEntity);
        }
        for (int i = 0; i < imgEntities.size(); i++) {
            int length = i < imgEntities.size() - 1 ? imgEntities.get(i + 1).getOffset() : stream.length();
            readImg(stream, imgEntities.get(i), length);
        }
        return imgEntities;
    }

    public static List<ImgEntity> readInfo(MemoryStream stream) {
        String flag = stream.readString();
        List<ImgEntity> imgEntities = new ArrayList<>();
        if (!StringUtils.equals(NPK_FlAG, flag)) {
            return imgEntities;
        }
        int count = stream.readInt();
        for (int i = 0; i < count; i++) {
            ImgEntity imgEntity = new ImgEntity();
            imgEntity.setOffset(stream.readInt());
            imgEntity.setLength(stream.readInt());
            imgEntity.setPath(readPath(stream));
            imgEntities.add(imgEntity);
        }
        return imgEntities;
    }

    public static void readImg(MemoryStream stream, ImgEntity imgEntity, long length) {
        stream.seek(imgEntity.getOffset(), MemoryStream.SeekOrigin.Begin);
        String albumFlag = stream.readString();
        if (StringUtils.equals(IMG_FLAG, albumFlag)) {
            imgEntity.setIndexLength(stream.readLong());
            imgEntity.setImgVersion(ImgVersions.valueOf(stream.readInt()));
            imgEntity.setCount(stream.readInt());
            imgEntity.initHandle(stream);
        } else {
            if (StringUtils.equals(IMAGE_FLAG, albumFlag)) {
                imgEntity.setImgVersion(ImgVersions.VERSION_1);
            } else {
                if (length < 0) {
                    length = stream.length();
                }
                imgEntity.setImgVersion(ImgVersions.OTHER);
                stream.seek(imgEntity.getOffset(), MemoryStream.SeekOrigin.Begin);
                if (StringUtils.endsWith(StringUtils.lowerCase(imgEntity.getName()), "ogg")) {
                    imgEntity.setImgVersion(ImgVersions.OTHER);
                    imgEntity.setIndexLength(length - stream.position());
                }
            }
            imgEntity.initHandle(stream);
        }
    }

    @SneakyThrows
    public static List<ImgEntity> load(boolean onlyPath, String file) {
        List<ImgEntity> imgEntities = new ArrayList<>();
        File path = new File(file);
        if (path.isDirectory()) {
            try (Stream<Path> walk = Files.walk(Paths.get(file))) {
                String[] array = walk.filter(Files::isRegularFile).map(Path::toString).toArray(String[]::new);
                return load(onlyPath, array);
            }
        }
        if (!path.isFile()) {
            return imgEntities;
        }
        byte[] fileBytes = Files.readAllBytes(Paths.get(file));
        MemoryStream stream = new MemoryStream(fileBytes.length);
        stream.write(fileBytes);
        if (onlyPath) {
            return readInfo(stream);
        }
        return readNpk(stream, file);
    }

    public static List<ImgEntity> load(boolean onlyPath, String[] files) {
        List<ImgEntity> imgEntities = new ArrayList<>();
        for (String file : files) {
            imgEntities.addAll(load(onlyPath, file));
        }
        return imgEntities;
    }

    private static String readPath(MemoryStream stream) {
        byte[] data = new byte[256];
        int i = 0;
        while (i < 256) {
            data[i] = (byte) (stream.readByte() ^ Key[i]);
            i++;
        }
        return StringUtils.toEncodedString(data, StandardCharsets.UTF_8).trim();
    }
}