package xyz.hooy.npkapi.util;

import xyz.hooy.npkapi.component.MemoryStream;
import xyz.hooy.npkapi.constant.AlbumModes;
import xyz.hooy.npkapi.entity.Album;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class NpkUtils {

    private NpkUtils() {
    }

    public static final String NPK_FlAG = "NeoplePack_Bill\0";

    public static final String IMG_FLAG = "Neople Img File\0";

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

    public static List<Album> readNpk(MemoryStream stream) {
        return readNpk(stream, null);
    }

    public static List<Album> readNpk(MemoryStream stream, String file) {
        List<Album> imgEntities = new ArrayList<>();
        String flag = stream.readString();
        if (NPK_FlAG.equals(flag)) {
            // 当文件是NPK时
            stream.seek(0, MemoryStream.SeekOrigin.Begin);
            imgEntities.addAll(readInfo(stream));
            if (!imgEntities.isEmpty()) {
                stream.seek(32, MemoryStream.SeekOrigin.Begin);
            }
        } else {
            Album album = new Album();
            if (Objects.nonNull(file)) {
                String suffix = file.substring(file.lastIndexOf(".") + 1);
                album.setPath(suffix);
            }
            imgEntities.add(album);
        }
        for (int i = 0; i < imgEntities.size(); i++) {
            int length = i < imgEntities.size() - 1 ? imgEntities.get(i + 1).getOffset() : stream.length();
            readImg(stream, imgEntities.get(i), length);
        }
        return imgEntities;
    }

    public static List<Album> readInfo(MemoryStream stream) {
        String flag = stream.readString();
        List<Album> imgEntities = new ArrayList<>();
        if (!NPK_FlAG.equals(flag)) {
            return imgEntities;
        }
        int count = stream.readInt();
        for (int i = 0; i < count; i++) {
            Album album = new Album();
            album.setOffset(stream.readInt());
            album.setLength(stream.readInt());
            album.setPath(readPath(stream));
            imgEntities.add(album);
        }
        return imgEntities;
    }

    public static void readImg(MemoryStream stream, Album album, long length) {
        stream.seek(album.getOffset(), MemoryStream.SeekOrigin.Begin);
        String albumFlag = stream.readString();
        if (IMG_FLAG.equals(albumFlag)) {
            album.setIndexLength(stream.readLong());
            album.setAlbumModes(AlbumModes.valueOf(stream.readInt()));
            album.setCount(stream.readInt());
            album.initHandle(stream);
        } else {
            if (IMAGE_FLAG.equals(albumFlag)) {
                album.setAlbumModes(AlbumModes.VERSION_1);
            } else {
                if (length < 0) {
                    length = stream.length();
                }
                album.setAlbumModes(AlbumModes.OGG);
                stream.seek(album.getOffset(), MemoryStream.SeekOrigin.Begin);
                if (album.getName().toLowerCase().endsWith("ogg")) {
                    album.setAlbumModes(AlbumModes.OGG);
                    album.setIndexLength(length - stream.position());
                }
            }
            album.initHandle(stream);
        }
    }

    public static void readImg(MemoryStream stream, Album album) {
        readImg(stream, album, -1);
    }

    public static Album readImg(MemoryStream stream, String path) {
        return readImg(stream, path, -1);
    }


    public static Album readImg(MemoryStream stream, String path, long length) {
        Album album = new Album();
        album.setPath(path);
        readImg(stream, album, length);
        return album;
    }


    public static void readImg(byte[] data, Album album) {
        readImg(data, album, -1);
    }

    public static Album readImg(byte[] data, String path) {
        return readImg(data, path, -1);
    }

    public static void readImg(byte[] data, Album album, long length) {
        MemoryStream ms = new MemoryStream(data.length);
        readImg(ms, album, length);
    }

    public static Album readImg(byte[] data, String path, long length) {
        MemoryStream ms = new MemoryStream(data.length);
        return readImg(ms, path, length);
    }

    public static void writeNpk(MemoryStream stream, List<Album> imgEntities) throws NoSuchAlgorithmException {
        int position = 52 + imgEntities.size() * 264;
        int length = 0;
        for (int i = 0; i < imgEntities.size(); i++) {
            imgEntities.get(i).adjust();
            if (i > 0) {
                if (Objects.nonNull(imgEntities.get(i).getTarget())) {
                    continue;
                }
                position += length;
            }
            imgEntities.get(i).setOffset(position);
            length = imgEntities.get(i).getLength();
        }
        imgEntities.forEach(ie -> {
            if (Objects.nonNull(ie.getTarget())) {
                ie.setOffset(ie.getTarget().getOffset());
                ie.setLength(ie.getTarget().getLength());
            }
        });
        MemoryStream ms = new MemoryStream();
        ms.writeString(NPK_FlAG);
        ms.writeInt(imgEntities.size());
        for (Album album : imgEntities) {
            ms.writeInt(album.getOffset());
            ms.writeInt(album.getLength());
            writePath(ms, album.getPath());
        }
        byte[] data = ms.toArray();
        stream.write(data);
        stream.write(compileHash(data));
        for (Album album : imgEntities) {
            if (Objects.isNull(album.getTarget())) {
                stream.write(album.getData());
            }
        }
    }

    public static List<Album> load(String file) throws IOException {
        return load(false, file);
    }

    public static List<Album> load(String[] files) throws IOException {
        return load(false, files);
    }

    public static List<Album> load(boolean onlyPath, String path) throws IOException {
        List<Album> imgEntities = new ArrayList<>();
        Path file = Paths.get(path);
        if (Files.isDirectory(file)) {
            try (Stream<Path> walk = Files.walk(file)) {
                String[] array = walk.filter(Files::isRegularFile).map(Path::toString).toArray(String[]::new);
                return load(onlyPath, array);
            }
        }
        if (!Files.isRegularFile(file)) {
            return imgEntities;
        }
        byte[] fileBytes = Files.readAllBytes(file);
        MemoryStream stream = new MemoryStream(fileBytes.length);
        stream.write(fileBytes);
        if (onlyPath) {
            return readInfo(stream);
        }
        return readNpk(stream, path);
    }

    public static List<Album> load(boolean onlyPath, String[] files) throws IOException {
        List<Album> imgEntities = new ArrayList<>();
        for (String file : files) {
            imgEntities.addAll(load(onlyPath, file));
        }
        return imgEntities;
    }

    public static void save(String path, List<Album> imgEntities) throws IOException, NoSuchAlgorithmException {
        Path file = Paths.get(path);
        if (!Files.isRegularFile(file)) {
            Files.createFile(file);
        }
        try (FileOutputStream fileOutputStream = new FileOutputStream(file.toFile());
             FileChannel fileChannel = fileOutputStream.getChannel()) {
            MemoryStream memoryStream = new MemoryStream();
            writeNpk(memoryStream, imgEntities);
            ByteBuffer buffer = memoryStream.getOnlyReadBuffer();
            while (buffer.hasRemaining()) {
                fileChannel.write(buffer);
            }
        }
    }

    private static String readPath(MemoryStream stream) {
        byte[] data = new byte[256];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (stream.readByte() ^ Key[i]);
        }
        return new String(data).trim();
    }

    private static void writePath(MemoryStream stream, String str) {
        byte[] data = new byte[256];
        byte[] valueBytes = str.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(valueBytes, 0, data, 0, valueBytes.length);
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (data[i] ^ Key[i]);
        }
        stream.write(data);
    }

    private static byte[] compileHash(byte[] data) throws NoSuchAlgorithmException {
        if (data.length == 0) {
            return new byte[0];
        }
        byte[] specimenBytes = new byte[data.length / 17 * 17];
        System.arraycopy(data, 0, specimenBytes, 0, specimenBytes.length);
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(specimenBytes);
        return messageDigest.digest();
    }
}