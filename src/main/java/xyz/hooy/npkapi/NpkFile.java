package xyz.hooy.npkapi;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * NPK 文件访问入口类。
 * <p>
 * NpkFile 是 npk-api 库的核心类，代表一个完整的 NPK 文件实例。
 * 提供 NPK 文件的打开、读取、资源管理和保存功能。
 * </p>
 * <p>
 * 资源以内存模型为单一事实来源：保存时一律通过
 * {@link NpkResource#write(javax.imageio.stream.ImageOutputStream)} 重新序列化，
 * 不依赖任何字节缓存，因此对资源（含其调色板、DDS 表等子对象）的
 * 任何修改都会在保存时如实写出。
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
     * NPK 文件魔数。
     * 包私有以避免公开可变数组被外部篡改；对外需要时使用 {@link #getNpkMagic()}。
     */
    static final byte[] NPK_MAGIC = {
            'N', 'e', 'o', 'p', 'l', 'e', 'P', 'a', 'c', 'k', '_', 'B', 'i', 'l', 'l', '\0'
    };

    /**
     * IMG 文件名解密密钥（XOR）。私有以避免密钥材料外泄为可篡改的公开数组。
     */
    private static final byte[] IMG_NAME_KEY = buildNameKey();

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

    /**
     * 获取 NPK 文件魔数的副本。
     */
    public static byte[] getNpkMagic() {
        return NPK_MAGIC.clone();
    }

    @Override
    public void close() {
        this.closed = true;
        this.resources.clear();
    }

    /**
     * 校验 NpkFile 未被关闭。
     * close() 会清空资源列表，此后继续操作（尤其 save 覆盖源文件）会静默丢失数据，故一律拒绝。
     */
    private void ensureOpen() {
        if (closed) {
            throw new NpkException("NpkFile is closed.");
        }
    }

    @Override
    public String toString() {
        // 不委托 getImages/getOggs（它们带 ensureOpen 校验），
        // 保证 toString 在已关闭实例上也可安全调用（调试器/日志场景）
        int imageCount = 0;
        int oggCount = 0;
        for (NpkResource resource : resources) {
            if (resource instanceof Img) {
                imageCount++;
            } else if (resource instanceof Ogg) {
                oggCount++;
            }
        }
        return "NpkFile{resources=" + resources.size()
                + ", images=" + imageCount
                + ", oggs=" + oggCount
                + (sourceFile != null ? ", file='" + sourceFile.getName() + "'" : "") + "}";
    }

    /**
     * 打开并解析一个 NPK 文件。
     * <p>
     * 读取 NPK 文件头、索引表和资源数据：名称以 {@code .ogg}（不区分大小写）结尾的
     * 资源视为 OGG 音频，其余按 IMG 解析并根据版本号创建对应处理器。
     * 索引表 SHA-256 校验失败或资源区间越界时视为文件损坏。
     * 所有资源数据会一次性全量读入内存。
     * </p>
     *
     * @param file NPK 文件
     * @return 解析后的 NpkFile 实例
     * @throws IOException  文件读取异常
     * @throws NpkException 文件不存在或格式错误
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
        ensureOpen();
        return Collections.unmodifiableList(resources);
    }

    /**
     * 获取所有 IMG 图像资源
     */
    public List<Img> getImages() {
        ensureOpen();
        return findResources(Img.class);
    }

    /**
     * 获取所有 OGG 音频资源
     */
    public List<Ogg> getOggs() {
        ensureOpen();
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
        ensureOpen();
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
        ensureOpen();
        return resources.size();
    }

    /**
     * 添加资源到 NPK 文件。
     * <p>
     * 名称须非空、经 ISO-8859-1 编码后不超过 255 字节且可无损编码
     * （中文名等会被拒绝），并且不得与既有资源重名（不区分大小写）；
     * OGG 资源必须已携带数据，否则无法序列化保存。
     * </p>
     *
     * @param resource IMG 或 OGG 资源
     * @throws NpkException 资源为 null、名称非法、名称重复或 OGG 资源缺少数据
     */
    public void addResource(NpkResource resource) {
        if (resource == null) {
            throw new NpkException("Resource cannot be null.");
        }
        ensureOpen();
        if (resource.getName() == null || resource.getName().isEmpty()) {
            throw new NpkException("Resource name cannot be null or empty.");
        }
        // 资源名经 ISO-8859-1 单字节编码后 XOR 加密写入 256 字节字段；
        // 超长名称会被静默截断、无法无损编码的字符（如中文）会被替换为 '?'，均须提前拒绝
        byte[] nameBytes = resource.getName().getBytes(StandardCharsets.ISO_8859_1);
        if (nameBytes.length > MAX_NAME_LENGTH) {
            throw new NpkException("Resource name too long: max " + MAX_NAME_LENGTH
                    + " bytes, got " + nameBytes.length + " (" + resource.getName() + ").");
        }
        if (!resource.getName().equals(new String(nameBytes, StandardCharsets.ISO_8859_1))) {
            throw new NpkException("Resource name contains characters not representable "
                    + "in ISO-8859-1: " + resource.getName());
        }
        if (findByName(resource.getName()) != null) {
            throw new NpkException("Duplicate resource name: " + resource.getName());
        }
        // fail-fast：OGG 以原始字节存储，缺数据时无法序列化，提前给出明确错误
        // （否则要等到 save() 才由 Ogg.write 抛出）
        if (resource instanceof Ogg && ((Ogg) resource).getData() == null) {
            throw new NpkException("OGG resource has no data: " + resource.getName());
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
        ensureOpen();
        return resources.remove(resource);
    }

    /**
     * 根据名称移除资源
     *
     * @param name 资源名称
     * @return 被移除的资源，未找到返回 null
     */
    public NpkResource removeByName(String name) {
        ensureOpen();
        NpkResource resource = findByName(name);
        if (resource != null) {
            resources.remove(resource);
        }
        return resource;
    }

    /**
     * 保存回打开时的源文件。
     *
     * @throws IOException  写入异常
     * @throws NpkException 本实例并非从文件打开（无源文件）或某个资源无法序列化
     * @see #save(File)
     */
    public void save() throws IOException {
        ensureOpen();
        if (sourceFile == null) {
            throw new NpkException("No source file to save to; use save(File) instead.");
        }
        save(sourceFile);
    }

    /**
     * 将 NPK 文件保存到指定路径。
     * <p>
     * 注意：本方法直接写入目标文件，写出过程中失败（如某个资源无法序列化）时
     * 目标文件可能已被部分写入而损坏，不会自动回滚；重要文件建议先保存到
     * 临时路径再替换。保存成功后，本实例的源文件将更新为该路径。
     * </p>
     *
     * @param file 目标文件
     * @throws IOException  写入异常
     * @throws NpkException 某个资源无法序列化时抛出
     */
    public void save(File file) throws IOException {
        ensureOpen();
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
        ensureOpen();
        return sourceFile;
    }

    private void read(File file) throws IOException {
        long fileLength = file.length();
        try (FileImageInputStream stream = new FileImageInputStream(file)) {
            stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);

            // 1. 验证魔数
            byte[] magic = new byte[NPK_MAGIC.length];
            stream.readFully(magic);
            if (!Arrays.equals(NPK_MAGIC, magic)) {
                throw new NpkException("Invalid NPK file: magic bytes mismatch. "
                        + "Expected 'NeoplePack_Bill\\0'.");
            }

            // 2. 读取资源数量
            int resourceCount = stream.readInt();
            if (resourceCount <= 0) {
                return; // 空 NPK 文件
            }

            // 防御：索引表 + 校验位必须落在文件范围内，否则视为损坏
            // （避免按损坏的 resourceCount 分配超大数组导致 OOM）
            long indexSize = (long) resourceCount * INDEX_ENTRY_SIZE;
            long requiredHeader = NPK_MAGIC.length + 4L + indexSize + VERIFICATION_SIZE;
            if (indexSize > Integer.MAX_VALUE || requiredHeader > fileLength) {
                throw new NpkException("Corrupt NPK: declared resource count " + resourceCount
                        + " requires " + requiredHeader + " bytes but file is only "
                        + fileLength + " bytes.");
            }

            // 3. 读取索引表（一次读入，条目解析直接基于该缓冲区，避免二次读流）
            byte[] indexBytes = new byte[(int) indexSize];
            stream.readFully(indexBytes);

            // 4. 读取校验哈希
            byte[] verification = new byte[VERIFICATION_SIZE];
            stream.readFully(verification);

            // 5. 验证完整性：校验范围为 NPK 文件头(data1) + 索引表(data2)
            verifyHash(indexBytes, resourceCount, verification);

            // 6. 解析每个资源索引项并读取其数据
            for (int i = 0; i < resourceCount; i++) {
                int entryBase = i * INDEX_ENTRY_SIZE;
                int offset = Bytes.readIntLE(indexBytes, entryBase);
                int length = Bytes.readIntLE(indexBytes, entryBase + 4);
                String name = decryptName(
                        Arrays.copyOfRange(indexBytes, entryBase + 8, entryBase + INDEX_ENTRY_SIZE));

                // 防御：offset/length 来自不可信文件，负值会触发 NegativeArraySizeException、
                // 过大或越界会读到错误数据甚至 OOM，统一提前校验为"文件损坏"
                if (offset < 0 || length < 0 || (long) offset + length > fileLength) {
                    throw new NpkException("Corrupt NPK: resource #" + i + " has invalid range ["
                            + offset + ", " + ((long) offset + length) + ") against file size "
                            + fileLength + ".");
                }

                // 跳转到资源数据位置读取
                stream.seek(offset);
                byte[] data = new byte[length];
                stream.readFully(data);

                resources.add(parseResource(name, data));
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

        // 解析 IMG 资源：先按固定偏移探测版本号，再交由对应版本的处理器完整解析
        if (data.length < AbstractImg.VERSION_OFFSET + 4) {
            throw new NpkException("Corrupt IMG resource '" + name + "': only " + data.length
                    + " bytes, too small to contain an IMG header.");
        }
        int version = Bytes.readIntLE(data, AbstractImg.VERSION_OFFSET);
        Img img = ImgFactory.create(version);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             MemoryCacheImageInputStream imgStream = new MemoryCacheImageInputStream(bais)) {
            imgStream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            img.read(imgStream);
        }
        img.setName(name);
        return img;
    }

    private void write(FileImageOutputStream stream) throws IOException {
        int count = resources.size();

        // 1. 写入魔数
        stream.write(NPK_MAGIC);

        // 2. 写入资源数量
        stream.writeInt(count);

        // 3. 先将所有资源从模型序列化（格式要求索引表先行，须先得到各资源长度）
        List<byte[]> resourceDataList = new ArrayList<>(count);
        for (NpkResource resource : resources) {
            resourceDataList.add(serializeResource(resource));
        }

        // 4. 构建索引表并写入
        int headerSize = NPK_MAGIC.length + 4 + count * INDEX_ENTRY_SIZE + VERIFICATION_SIZE;
        byte[] indexBytes = new byte[count * INDEX_ENTRY_SIZE];
        int currentOffset = headerSize;
        int idx = 0;
        for (int i = 0; i < count; i++) {
            byte[] data = resourceDataList.get(i);
            Bytes.writeIntLE(indexBytes, idx, currentOffset);
            Bytes.writeIntLE(indexBytes, idx + 4, data.length);
            byte[] encryptedName = encryptName(resources.get(i).getName());
            System.arraycopy(encryptedName, 0, indexBytes, idx + 8, NAME_FIELD_SIZE);
            idx += INDEX_ENTRY_SIZE;
            currentOffset += data.length;
        }
        stream.write(indexBytes);

        // 5. 写入校验哈希：校验范围为 NPK 文件头(data1) + 索引表(data2)
        stream.write(computeVerification(indexBytes, count));

        // 6. 写入资源数据
        for (byte[] data : resourceDataList) {
            stream.write(data);
        }
    }

    /**
     * 将资源从内存模型完整序列化（不依赖任何字节缓存）。
     */
    private byte[] serializeResource(NpkResource resource) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream out = new MemoryCacheImageOutputStream(baos)) {
            out.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            resource.write(out);
            out.flush();
        }
        return baos.toByteArray();
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

    private void verifyHash(byte[] indexBytes, int resourceCount, byte[] expectedHash) {
        byte[] computed = computeVerification(indexBytes, resourceCount);
        // 恒定时间比较，同时避免手写逐字节循环
        if (!MessageDigest.isEqual(computed, expectedHash)) {
            throw new NpkException("NPK file verification failed: index table hash mismatch. "
                    + "The file may be corrupted or modified.");
        }
    }

    /**
     * 计算 NPK 校验位输入：拼接「NPK 文件头(魔数+资源数量) + 索引表」。
     */
    private static byte[] computeVerification(byte[] indexBytes, int resourceCount) {
        byte[] headerBytes = new byte[NPK_MAGIC.length + 4];
        System.arraycopy(NPK_MAGIC, 0, headerBytes, 0, NPK_MAGIC.length);
        Bytes.writeIntLE(headerBytes, NPK_MAGIC.length, resourceCount);
        byte[] combined = new byte[headerBytes.length + indexBytes.length];
        System.arraycopy(headerBytes, 0, combined, 0, headerBytes.length);
        System.arraycopy(indexBytes, 0, combined, headerBytes.length, indexBytes.length);
        return computeHash(combined);
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
