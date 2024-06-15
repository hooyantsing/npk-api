package xyz.hooy.npkapi.npk;

import xyz.hooy.npkapi.component.MemoryStream;
import xyz.hooy.npkapi.npk.constant.AlbumModes;
import xyz.hooy.npkapi.npk.entity.Album;

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

public final class NpkCore {

    private NpkCore() {
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
        List<Album> albums = new ArrayList<>();
        String flag = stream.readString();
        if (NPK_FlAG.equals(flag)) {
            // 当文件是NPK时
            stream.seek(0, MemoryStream.SeekOrigin.Begin);
            albums.addAll(readInfo(stream));
            if (!albums.isEmpty()) {
                stream.seek(32, MemoryStream.SeekOrigin.Current);
            }
        } else {
            Album album = new Album();
            if (Objects.nonNull(file)) {
                String suffix = file.substring(file.lastIndexOf(".") + 1);
                album.setPath(suffix);
            }
            albums.add(album);
        }
        for (int i = 0; i < albums.size(); i++) {
            int length = i < albums.size() - 1 ? albums.get(i + 1).getOffset() : stream.length();
            readImg(stream, albums.get(i), length);
        }
        return albums;
    }

    public static List<Album> readInfo(MemoryStream stream) {
        String flag = stream.readString();
        List<Album> albums = new ArrayList<>();
        if (!NPK_FlAG.equals(flag)) {
            return albums;
        }
        int count = stream.readInt();
        for (int i = 0; i < count; i++) {
            Album album = new Album();
            album.setOffset(stream.readInt());
            album.setLength(stream.readInt());
            album.setPath(readPath(stream));
            albums.add(album);
        }
        return albums;
    }

    public static void readImg(MemoryStream stream, Album album, int length) {
        stream.seek(album.getOffset(), MemoryStream.SeekOrigin.Begin);
        String albumFlag = stream.readString();
        if (IMG_FLAG.equals(albumFlag)) {
            album.setIndexLength(Math.toIntExact(stream.readLong()));
            album.setAlbumModes(AlbumModes.valueOf(stream.readInt()));
            album.setCount(stream.readInt());
            album.initHandle(stream);
        } else if (IMAGE_FLAG.equals(albumFlag)) {
            album.setAlbumModes(AlbumModes.VERSION_1);
            album.initHandle(stream);
        } else if (album.getName().toLowerCase().endsWith("ogg")) {
            if (length < 0) {
                length = stream.length();
            }
            stream.seek(album.getOffset(), MemoryStream.SeekOrigin.Begin);
            album.setAlbumModes(AlbumModes.OGG);
            album.setLength(length - stream.position());
            album.setIndexLength(0);
            album.initHandle(stream);
            album.adjust();
        }
    }

    public static void readImg(MemoryStream stream, Album album) {
        readImg(stream, album, -1);
    }

    public static Album readImg(MemoryStream stream, String path) {
        return readImg(stream, path, -1);
    }


    public static Album readImg(MemoryStream stream, String path, int length) {
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

    public static void readImg(byte[] data, Album album, int length) {
        MemoryStream ms = new MemoryStream(data.length);
        readImg(ms, album, length);
    }

    public static Album readImg(byte[] data, String path, int length) {
        MemoryStream ms = new MemoryStream(data.length);
        return readImg(ms, path, length);
    }

    public static void writeNpk(MemoryStream stream, List<Album> albums) throws NoSuchAlgorithmException {
        int position = 52 + albums.size() * 264;
        int length = 0;
        for (int i = 0; i < albums.size(); i++) {
            albums.get(i).adjust();
            if (i > 0) {
                if (Objects.nonNull(albums.get(i).getTarget())) {
                    continue;
                }
                position += length;
            }
            albums.get(i).setOffset(position);
            length = albums.get(i).getLength();
        }
        albums.forEach(ie -> {
            if (Objects.nonNull(ie.getTarget())) {
                ie.setOffset(ie.getTarget().getOffset());
                ie.setLength(ie.getTarget().getLength());
            }
        });
        MemoryStream ms = new MemoryStream();
        ms.writeString(NPK_FlAG);
        ms.writeInt(albums.size());
        for (Album album : albums) {
            ms.writeInt(album.getOffset());
            ms.writeInt(album.getLength());
            writePath(ms, album.getPath());
        }
        byte[] data = ms.toArray();
        stream.write(data);
        stream.write(compileHash(data));
        for (Album album : albums) {
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
        List<Album> albums = new ArrayList<>();
        Path file = Paths.get(path);
        if (Files.isDirectory(file)) {
            try (Stream<Path> walk = Files.walk(file)) {
                String[] array = walk.filter(Files::isRegularFile).map(Path::toString).toArray(String[]::new);
                return load(onlyPath, array);
            }
        }
        if (!Files.isRegularFile(file)) {
            return albums;
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
        List<Album> albums = new ArrayList<>();
        for (String file : files) {
            albums.addAll(load(onlyPath, file));
        }
        return albums;
    }

    public static void save(String path, List<Album> albums) throws IOException, NoSuchAlgorithmException {
        Path file = Paths.get(path);
        if (!Files.isRegularFile(file)) {
            Files.createFile(file);
        }
        try (FileOutputStream fileOutputStream = new FileOutputStream(file.toFile());
             FileChannel fileChannel = fileOutputStream.getChannel()) {
            MemoryStream memoryStream = new MemoryStream();
            writeNpk(memoryStream, albums);
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