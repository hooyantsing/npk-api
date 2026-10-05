package xyz.hooy.npkit;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * NPK 文件访问入口类。
 * <p>
 * NpkFile 是 npk-api 库的核心类，代表一个完整的 NPK 文件实例。
 * 提供 NPK 文件的打开、读取、资源管理和保存功能。
 * </p>
 *
 * <h3>NPK 文件格式概述</h3>
 * <pre>
 * [Header]
 *   - NPK_MAGIC (16 bytes): "NeoplePack_Bill\0"
 *   - resourceCount (int32)
 *   - resources[resourceCount] (264 bytes each):
 *     - offset (int32)
 *     - length (int32)
 *     - name (256 bytes, encrypted)
 * [Verification Hash] (32 bytes)
 * [Resource Data...]
 * </pre>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 打开并读取 NPK 文件
 * try (NpkFile npk = NpkFile.open(new File("character.npk"))) {
 *     // 获取所有 IMG 资源
 *     for (Img img : npk.getImages()) {
 *         System.out.println(img.getName() + ": " + img.getImageCount() + " frames");
 *         BufferedImage frame0 = img.getImage(0);
 *     }
 *
 *     // 获取所有 OGG 资源
 *     for (Ogg ogg : npk.getOggs()) {
 *         System.out.println(ogg.getName() + ": " + ogg.getSize() + " bytes");
 *     }
 * }
 *
 * // 创建新 NPK 文件
 * NpkFile npk = NpkFile.create();
 * npk.addResource(img);
 * npk.addResource(ogg);
 * npk.save(new File("output.npk"));
 * }</pre>
 *
 * @see Img
 * @see Ogg
 * @see ImgFactory
 */
public class NpkFile implements AutoCloseable {

    /**
     * NPK 文件魔数
     */
    public static final byte[] NPK_MAGIC = {
            'N', 'e', 'o', 'p', 'l', 'e', 'P', 'a', 'c', 'k', '_', 'B', 'i', 'l', 'l', '\0'
    };

    /**
     * IMG 文件名解密密钥
     */
    public static final byte[] IMG_NAME_KEY = buildNameKey();

    /**
     * 每个资源索引项的固定大小（offset + length + name = 4 + 4 + 256）
     */
    private static final int INDEX_ENTRY_SIZE = 264;

    /**
     * 资源名称字段大小（字节）
     */
    private static final int NAME_FIELD_SIZE = 256;

    /**
     * 资源名称最大长度（{@link #NAME_FIELD_SIZE} - 1，末字节保留为 null 终止符）
     */
    private static final int MAX_NAME_LENGTH = NAME_FIELD_SIZE - 1;

    /**
     * 校验哈希长度
     */
    private static final int VERIFICATION_SIZE = 32;

    private final List<NpkResource> resources = new ArrayList<>();
    private File sourceFile;
    private boolean closed = false;

    private NpkFile() {
    }

    private NpkFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    @Override
    public void close() {
        this.closed = true;
        this.resources.clear();
    }

    @Override
    public String toString() {
        return "NpkFile{resources=" + resources.size()
                + ", images=" + getImages().size()
                + ", oggs=" + getOggs().size()
                + (sourceFile != null ? ", file='" + sourceFile.getName() + "'" : "") + "}";
    }

    /**
     * 打开并解析一个 NPK 文件。
     * <p>
     * 读取 NPK 文件头、索引表和资源数据，自动识别 IMG/OGG 类型
     * 并根据 IMG 版本号创建对应的处理器。
     * </p>
     *
     * @param file NPK 文件
     * @return 解析后的 NpkFile 实例
     * @throws IOException  文件读取异常
     * @throws NpkException 文件格式错误
     */
    public static NpkFile open(File file) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new NpkException("NPK file not found: " + file.getAbsolutePath());
        }

        NpkFile npk = new NpkFile(file);
        npk.read(file);
        return npk;
    }

    /**
     * 创建一个空的 NpkFile 实例，用于构建新的 NPK 文件。
     *
     * @return 空的 NpkFile 实例
     */
    public static NpkFile create() {
        return new NpkFile();
    }

    /**
     * 获取所有资源（IMG + OGG）
     */
    public List<NpkResource> getResources() {
        return Collections.unmodifiableList(resources);
    }

    /**
     * 获取所有 IMG 图像资源
     */
    public List<Img> getImages() {
        return findResources(Img.class);
    }

    /**
     * 获取所有 OGG 音频资源
     */
    public List<Ogg> getOggs() {
        return findResources(Ogg.class);
    }

    /**
     * 按资源类型过滤当前资源列表
     */
    private <T extends NpkResource> List<T> findResources(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (NpkResource resource : resources) {
            if (type.isInstance(resource)) {
                result.add(type.cast(resource));
            }
        }
        return result;
    }

    /**
     * 根据资源名称查找资源
     *
     * @param name 资源路径名（不区分大小写）
     * @return 匹配的资源，未找到返回 null
     */
    public NpkResource findByName(String name) {
        for (NpkResource resource : resources) {
            if (resource.getName().equalsIgnoreCase(name)) {
                return resource;
            }
        }
        return null;
    }

    /**
     * 获取资源总数
     */
    public int getResourceCount() {
        return resources.size();
    }

    /**
     * 添加资源到 NPK 文件
     *
     * @param resource IMG 或 OGG 资源
     */
    public void addResource(NpkResource resource) {
        if (resource.getName() == null || resource.getName().isEmpty()) {
            throw new NpkException("Resource name cannot be null or empty.");
        }
        if (findByName(resource.getName()) != null) {
            throw new NpkException("Duplicate resource name: " + resource.getName());
        }
        resources.add(resource);
    }

    /**
     * 移除指定资源
     *
     * @param resource 要移除的资源
     * @return 是否成功移除
     */
    public boolean removeResource(NpkResource resource) {
        return resources.remove(resource);
    }

    /**
     * 根据名称移除资源
     *
     * @param name 资源名称
     * @return 被移除的资源，未找到返回 null
     */
    public NpkResource removeByName(String name) {
        NpkResource resource = findByName(name);
        if (resource != null) {
            resources.remove(resource);
        }
        return resource;
    }

    /**
     * 将 NPK 文件保存到指定路径
     *
     * @param file 目标文件
     * @throws IOException 写入异常
     */
    public void save(File file) throws IOException {
        try (FileImageOutputStream stream = new FileImageOutputStream(file)) {
            stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            write(stream);
        }
        this.sourceFile = file;
    }

    /**
     * 获取源文件（如果是从文件打开的）
     */
    public File getSourceFile() {
        return sourceFile;
    }

    private void read(File file) throws IOException {
        try (FileImageInputStream stream = new FileImageInputStream(file)) {
            stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);

            // 1. 验证魔数
            byte[] magic = new byte[NPK_MAGIC.length];
            stream.readFully(magic);
            verifyMagic(magic);

            // 2. 读取资源数量
            int resourceCount = stream.readInt();
            if (resourceCount <= 0) {
                return; // 空 NPK 文件
            }

            // 3. 读取索引表
            stream.mark();
            byte[] indexBytes = new byte[resourceCount * INDEX_ENTRY_SIZE];
            stream.readFully(indexBytes);

            // 4. 读取校验哈希
            byte[] verification = new byte[VERIFICATION_SIZE];
            stream.readFully(verification);

            // 5. 验证完整性：校验范围为 NPK 文件头(data1) + 索引表(data2)
            verifyHash(indexBytes, resourceCount, verification);
            stream.reset();

            // 6. 解析每个资源
            for (int i = 0; i < resourceCount; i++) {
                int offset = stream.readInt();
                int length = stream.readInt();
                byte[] nameBytes = new byte[256];
                stream.readFully(nameBytes);
                String name = decryptName(nameBytes);

                // 跳转到资源数据位置读取
                stream.mark();
                stream.seek(offset);
                byte[] data = new byte[length];
                stream.readFully(data);
                stream.reset();

                // 根据扩展名判断资源类型
                NpkResource resource = parseResource(name, data);
                resources.add(resource);
            }
        }
    }

    /**
     * 根据文件名和数据解析资源
     */
    private NpkResource parseResource(String name, byte[] data) throws IOException {
        if (name.toLowerCase().endsWith(".ogg")) {
            return new Ogg(name, data);
        }

        // 解析 IMG 资源
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             MemoryCacheImageInputStream imgStream = new MemoryCacheImageInputStream(bais)) {
            imgStream.setByteOrder(ByteOrder.LITTLE_ENDIAN);

            // 跳过魔数(16) + tableLength(4) + padding(4) 读取版本号
            imgStream.skipBytes(AbstractImg.IMG_MAGIC.length + 8);
            int version = imgStream.readInt();
            imgStream.seek(0);

            Img img = ImgFactory.create(version);
            img.read(imgStream);
            img.setName(name);
            img.setRawData(data);
            return img;
        }
    }

    private void write(FileImageOutputStream stream) throws IOException {
        int count = resources.size();

        // 1. 写入魔数
        stream.write(NPK_MAGIC);

        // 2. 写入资源数量
        stream.writeInt(count);

        // 3. 计算索引表并写入
        int headerSize = NPK_MAGIC.length + 4 + count * INDEX_ENTRY_SIZE + VERIFICATION_SIZE;
        byte[] indexBytes = new byte[count * INDEX_ENTRY_SIZE];
        int currentOffset = headerSize;

        // 先将所有资源数据序列化
        List<byte[]> resourceDataList = new ArrayList<>(count);
        for (NpkResource resource : resources) {
            byte[] data = serializeResource(resource);
            resourceDataList.add(data);
        }

        // 构建索引表
        int idx = 0;
        for (int i = 0; i < count; i++) {
            byte[] data = resourceDataList.get(i);
            writeIntLE(indexBytes, idx, currentOffset);
            idx += 4;
            writeIntLE(indexBytes, idx, data.length);
            idx += 4;
            byte[] encryptedName = encryptName(resources.get(i).getName());
            System.arraycopy(encryptedName, 0, indexBytes, idx, 256);
            idx += 256;
            currentOffset += data.length;
        }

        stream.write(indexBytes);

        // 4. 写入校验哈希：校验范围为 NPK 文件头(data1) + 索引表(data2)
        stream.write(computeVerification(indexBytes, count));

        // 5. 写入资源数据
        for (byte[] data : resourceDataList) {
            stream.write(data);
        }
    }

    private byte[] serializeResource(NpkResource resource) throws IOException {
        if (resource.getRawData() != null) {
            return resource.getRawData();
        }
        if (resource instanceof Img) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (MemoryCacheImageOutputStream out = new MemoryCacheImageOutputStream(baos)) {
                out.setByteOrder(ByteOrder.LITTLE_ENDIAN);
                ((Img) resource).write(out);
                out.flush();
            }
            return baos.toByteArray();
        }
        throw new NpkException("Cannot serialize resource: " + resource.getName());
    }

    /**
     * 解密资源名称（XOR 解密）
     */
    static String decryptName(byte[] encrypted) {
        byte[] decrypted = new byte[encrypted.length];
        for (int i = 0; i < encrypted.length; i++) {
            decrypted[i] = (byte) (encrypted[i] ^ IMG_NAME_KEY[i % IMG_NAME_KEY.length]);
        }
        // 找到 null 终止符
        int len = 0;
        while (len < decrypted.length && decrypted[len] != '\0') {
            len++;
        }
        return new String(decrypted, 0, len, StandardCharsets.ISO_8859_1);
    }

    /**
     * 加密资源名称（XOR 加密）
     */
    static byte[] encryptName(String name) {
        byte[] result = new byte[NAME_FIELD_SIZE];
        byte[] nameBytes = name.getBytes(StandardCharsets.ISO_8859_1);
        int filled = Math.min(nameBytes.length, MAX_NAME_LENGTH);
        for (int i = 0; i < filled; i++) {
            result[i] = (byte) (nameBytes[i] ^ IMG_NAME_KEY[i % IMG_NAME_KEY.length]);
        }
        // 剩余字节用 key 加密 '\0'
        for (int i = filled; i < NAME_FIELD_SIZE; i++) {
            result[i] = IMG_NAME_KEY[i % IMG_NAME_KEY.length];
        }
        return result;
    }

    private void verifyMagic(byte[] magic) {
        for (int i = 0; i < NPK_MAGIC.length; i++) {
            if (magic[i] != NPK_MAGIC[i]) {
                throw new NpkException("Invalid NPK file: magic bytes mismatch. "
                        + "Expected 'NeoplePack_Bill\\0'.");
            }
        }
    }

    private void verifyHash(byte[] indexBytes, int resourceCount, byte[] expectedHash) {
        byte[] computed = computeVerification(indexBytes, resourceCount);
        for (int i = 0; i < VERIFICATION_SIZE; i++) {
            if (computed[i] != expectedHash[i]) {
                throw new NpkException("NPK file verification failed: index table hash mismatch. "
                        + "The file may be corrupted or modified.");
            }
        }
    }

    /**
     * 计算 NPK 校验位输入：拼接「NPK 文件头(魔数+资源数量) + 索引表」。
     */
    private static byte[] computeVerification(byte[] indexBytes, int resourceCount) {
        byte[] headerBytes = new byte[NPK_MAGIC.length + 4];
        System.arraycopy(NPK_MAGIC, 0, headerBytes, 0, NPK_MAGIC.length);
        writeIntLE(headerBytes, NPK_MAGIC.length, resourceCount);
        return computeHash(concat(headerBytes, indexBytes));
    }

    /**
     * 计算 NPK 校验位。
     * <p>
     * 依据 NPK 格式规范：校验位为「NPK 文件头 + IMG 索引表」拼接字节流的
     * 前 17 的整数倍部分，进行 SHA-256 加密后得到的 32 字节结果。
     * </p>
     *
     * @param data NPK 文件头(data1) 与索引表(data2) 拼接后的字节流
     * @return 32 字节 SHA-256 校验位
     */
    private static byte[] computeHash(byte[] data) {
        // 截断到 17 字节的整数倍
        int len = data.length / 17 * 17;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(data, 0, len);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new NpkException("SHA-256 algorithm not available.", e);
        }
    }

    /**
     * 拼接两个字节数组。
     */
    private static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    private static void writeIntLE(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) ((value >> 8) & 0xFF);
        buffer[offset + 2] = (byte) ((value >> 16) & 0xFF);
        buffer[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }

    /**
     * 构建 IMG 文件名解密密钥
     */
    private static byte[] buildNameKey() {
        byte[] key = new byte[NAME_FIELD_SIZE];
        String base = "puchikon@neople dungeon and fighter DNF";
        byte[] baseBytes = base.getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(baseBytes, 0, key, 0, baseBytes.length);
        // 后续填充 "DNF" 循环
        String suffix = "DNF";
        byte[] suffixBytes = suffix.getBytes(StandardCharsets.ISO_8859_1);
        int pos = baseBytes.length;
        int si = 0;
        while (pos < MAX_NAME_LENGTH) {
            key[pos++] = suffixBytes[si % suffixBytes.length];
            si++;
        }
        key[MAX_NAME_LENGTH] = '\0';
        return key;
    }
}
