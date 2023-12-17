package xyz.hooy.npkapi.coder;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import xyz.hooy.npkapi.constant.ImgVersions;
import xyz.hooy.npkapi.entity.ImgEntity;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class NpkCoder {

    public static Charset Encoding = StandardCharsets.UTF_8;

    public static final String NPK_FlAG = "NeoplePack_Bill";

    public static final Integer NPK_FLAG_LENGTH = 16;

    public static final String IMG_FLAG = "Neople Img File";

    public static final Integer IMG_FLAG_LENGTH = 16;

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
            "DNFDNFDNF\0").getBytes(Encoding);

    public static List<ImgEntity> readNpk(ByteBuffer buffer) {
        return readNpk(buffer, null);
    }

    public static List<ImgEntity> readNpk(ByteBuffer buffer, String file) {
        List<ImgEntity> imgEntities = new ArrayList<>();
        byte[] flagBytes = new byte[NPK_FLAG_LENGTH];
        buffer.get(flagBytes);
        String flag = StringUtils.toEncodedString(flagBytes, Encoding).trim();
        if (StringUtils.equals(NPK_FlAG, flag)) {
            // 当文件是NPK时
            buffer.position(0);
            imgEntities.addAll(readInfo(buffer));
            if (!imgEntities.isEmpty()) {
                buffer.position(32);
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
            int length = i < imgEntities.size() - 1 ? imgEntities.get(i + 1).getOffset() : buffer.array().length;
            readImg(buffer, imgEntities.get(i), length);
        }
        return imgEntities;
    }

    public static List<ImgEntity> readInfo(ByteBuffer buffer) {
        byte[] flagBytes = new byte[NPK_FLAG_LENGTH];
        buffer.get(flagBytes);
        String flag = StringUtils.toEncodedString(flagBytes, Encoding).trim();
        List<ImgEntity> imgEntities = new ArrayList<>();
        if (!StringUtils.equals(NPK_FlAG, flag)) {
            return imgEntities;
        }
        int count = buffer.getInt();
        for (int i = 0; i < count; i++) {
            ImgEntity imgEntity = new ImgEntity();
            imgEntity.setOffset(buffer.getInt());
            imgEntity.setLength(buffer.getInt());
            imgEntity.setPath(readPath(buffer));
            imgEntities.add(imgEntity);
        }
        return imgEntities;
    }

    public static void readImg(ByteBuffer buffer, ImgEntity imgEntity, long length) {
        buffer.position(imgEntity.getOffset());
        byte[] albumFlagBytes = new byte[IMG_FLAG_LENGTH];
        buffer.get(albumFlagBytes);
        String albumFlag = StringUtils.toEncodedString(albumFlagBytes, Encoding).trim();
        if (StringUtils.equals(IMG_FLAG, albumFlag)) {
            imgEntity.setIndexLength(buffer.getLong());
            imgEntity.setImgVersion(ImgVersions.valueOf(buffer.getInt()));
            imgEntity.setCount(buffer.getInt());
            imgEntity.initHandle(buffer);
        } else {
            if (StringUtils.equals(IMAGE_FLAG, albumFlag)) {
                imgEntity.setImgVersion(ImgVersions.VERSION_1);
            } else {
                if (length < 0) {
                    length = buffer.array().length;
                }
                imgEntity.setImgVersion(ImgVersions.OTHER);
                buffer.position(imgEntity.getOffset());
                if (StringUtils.endsWith(StringUtils.lowerCase(imgEntity.getName()), "ogg")) {
                    imgEntity.setImgVersion(ImgVersions.OTHER);
                    imgEntity.setIndexLength(length - buffer.position());
                }
            }
            imgEntity.initHandle(buffer);
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
        ByteBuffer buffer = ByteBuffer.allocate(fileBytes.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(fileBytes);
        buffer.flip();
        if (onlyPath) {
            return readInfo(buffer);
        }
        return readNpk(buffer, file);
    }

    public static List<ImgEntity> load(boolean onlyPath, String[] files) {
        List<ImgEntity> imgEntities = new ArrayList<>();
        for (String file : files) {
            imgEntities.addAll(load(onlyPath, file));
        }
        return imgEntities;
    }

    private static String readPath(ByteBuffer buffer) {
        byte[] data = new byte[256];
        int i = 0;
        while (i < 256) {
            data[i] = (byte) (buffer.get() ^ Key[i]);
            i++;
        }
        return StringUtils.toEncodedString(data, Encoding).trim();
    }
}