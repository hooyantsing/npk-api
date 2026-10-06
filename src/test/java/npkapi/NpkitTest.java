package npkapi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.hooy.npkit.*;
import xyz.hooy.npkit.Frame;
import xyz.hooy.npkit.codec.Argb8888Codec;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NpkFile 写入功能测试。
 */
public class NpkitTest {

    @Test
    void testNpkFileCreateAndWrite(@TempDir Path tempDir) throws IOException {
        // 构建 IMG 资源
        Version2Img img = new Version2Img();
        img.setName("character/swordman/effect.img");
        BufferedImage testImage = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        img.addImage(testImage, Frame.TYPE_ARGB8888);
        img.addImage(testImage, Frame.TYPE_ARGB1555);
        img.addReferenceImage(0);

        // 构建 OGG 资源
        Ogg ogg = new Ogg("sound/swordman/attack.ogg", new byte[256]);

        // 通过 NpkFile 写入
        NpkFile npk = NpkFile.create();
        npk.addResource(img);
        npk.addResource(ogg);
        Path outputPath = tempDir.resolve("test_output.npk");
        npk.save(outputPath.toFile());

        assertTrue(Files.exists(outputPath));
        assertTrue(Files.size(outputPath) > 0);
    }

    @Test
    void testNpkFileSaveAndReopen(@TempDir Path tempDir) throws IOException {
        // 创建并保存
        NpkFile npk = NpkFile.create();
        Version4Img img = new Version4Img();
        img.setName("equip/weapon.img");

        // V4 需要调色板，先手动设置
        BufferedImage testImage = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        img.addImage(testImage, Frame.TYPE_INDEXED);

        npk.addResource(img);
        npk.addResource(new Ogg("bgm/stage1.ogg", new byte[512]));

        Path outputPath = tempDir.resolve("saved.npk");
        npk.save(outputPath.toFile());
        assertTrue(Files.exists(outputPath));
    }

    @Test
    void testCodecRegistration() {
        // 验证默认编解码器注册
        Version2Img v2 = new Version2Img();
        assertNotNull(v2);

        // 验证 V5 具备 DDS 能力，其余版本降级（无 DDS 表 / 抛异常）
        Version5Img v5 = new Version5Img();
        assertEquals(5, v5.getVersion());
        assertTrue(v5.supportDdsSource());
        assertEquals(0, v5.getDdsSourceCount());
        assertNull(v5.getDdsSource(0));

        assertFalse(v2.supportDdsSource());
        assertFalse(new Version4Img().supportDdsSource());
        assertFalse(new Version6Img().supportDdsSource());
        assertEquals(0, v2.getDdsSourceCount());
        assertThrows(NpkException.class, () -> v2.addDdsSource(new DdsSource(new byte[1], Frame.TYPE_FXT1, 4, 4)));
        assertThrows(NpkException.class, () -> new Version4Img().addDdsImage(0, 0, 0, 4, 4, 0, 0, 4, 4));
        assertThrows(NpkException.class, () -> new Version6Img().addDdsImage(0, 0, 0, 4, 4, 0, 0, 4, 4));
    }

    @Test
    void testPaletteCapable() {
        // V4/V6 具备调色板能力，其余版本降级（数量为 0 / 抛异常）
        Version4Img v4 = new Version4Img();
        assertTrue(v4.supportPaletteSource());
        assertEquals(1, v4.getPaletteSourceCount());
        assertEquals(0, v4.getActivePaletteSourceIndex());
        assertNotNull(v4.getPaletteSource(0));
        assertThrows(IndexOutOfBoundsException.class, () -> v4.activatePaletteSource(1));
        assertThrows(IndexOutOfBoundsException.class, () -> v4.getPaletteSource(1));

        Version2Img v2 = new Version2Img();
        assertFalse(v2.supportPaletteSource());
        assertEquals(0, v2.getPaletteSourceCount());
        assertThrows(NpkException.class, () -> v2.getActivePaletteSourceIndex());
        assertThrows(NpkException.class, () -> v2.activatePaletteSource(0));
        assertThrows(NpkException.class, () -> v2.getPaletteSource(0));
        assertThrows(NpkException.class, () -> v2.addPalettesImage(new BufferedImage[]{new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB)}));

        Version5Img v5 = new Version5Img();
        assertFalse(v5.supportPaletteSource());
        assertEquals(0, v5.getPaletteSourceCount());
        assertThrows(NpkException.class, () -> v5.activatePaletteSource(0));

        // 多调色板能力仅 V6 具备，V4 不具备
        Version6Img v6 = new Version6Img();
        assertTrue(v6.supportPaletteSource());
        assertEquals(1, v6.getPaletteSourceCount());
        assertEquals(0, v6.getActivePaletteSourceIndex());
        assertNotNull(v6.getPaletteSource(0));
        assertThrows(IndexOutOfBoundsException.class, () -> v6.activatePaletteSource(1));
        assertThrows(IndexOutOfBoundsException.class, () -> v6.getPaletteSource(1));
    }

    @Test
    void testCustomCodecRegistration() {
        // 验证 ImgFactory 支持自定义版本注册
        assertFalse(ImgFactory.isSupported(99));
        try {
            ImgFactory.register(99, Version2Img::new); // 注册一个测试版本
            assertTrue(ImgFactory.isSupported(99));
            Img custom = ImgFactory.create(99);
            assertNotNull(custom);
        } finally {
            // 清理全局静态注册表，避免污染其他测试
            ImgFactory.unregister(99);
        }
        assertFalse(ImgFactory.isSupported(99));
    }

    @Test
    void testArgbCodecRoundTrip() {
        // 测试 ARGB8888 编解码往返
        Argb8888Codec codec = new Argb8888Codec();
        BufferedImage original = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        // 设置一些像素
        original.setRGB(0, 0, 0xFFFF0000); // 红色
        original.setRGB(1, 1, 0xFF00FF00); // 绿色
        original.setRGB(2, 2, 0xFF0000FF); // 蓝色

        Frame frame = codec.encode(original, null);
        assertNotNull(frame);
        assertEquals(Frame.TYPE_ARGB8888, frame.getType());
        assertEquals(4, frame.getWidth());
        assertEquals(4, frame.getHeight());

        BufferedImage decoded = codec.decode(frame, null);
        assertNotNull(decoded);
        assertEquals(4, decoded.getWidth());
        assertEquals(4, decoded.getHeight());
    }

    @Test
    void testVersion4IndexedRoundTrip(@TempDir Path tempDir) throws IOException {
        // 索引色 V4：写出→重读→逐像素还原（同时覆盖资源名加解密往返）
        Version4Img img = new Version4Img();
        img.setName("test/indexed.img");
        BufferedImage src = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        src.setRGB(0, 0, 0xFFFF0000); // 红
        src.setRGB(1, 1, 0xFF00FF00); // 绿
        src.setRGB(2, 2, 0xFF0000FF); // 蓝
        img.addImage(src, Frame.TYPE_INDEXED);
        assertEquals(1, img.getImageCount());

        NpkFile npk = NpkFile.create();
        npk.addResource(img);
        Path out = tempDir.resolve("v4_indexed.npk");
        npk.save(out.toFile());

        try (NpkFile reopened = NpkFile.open(out.toFile())) {
            Img loaded = reopened.getImages().get(0);
            assertEquals("test/indexed.img", loaded.getName());
            assertEquals(4, loaded.getVersion());
            assertEquals(1, loaded.getImageCount());
            BufferedImage decoded = loaded.getImage(0);
            assertEquals(4, decoded.getWidth());
            assertEquals(4, decoded.getHeight());
            assertEquals(0xFFFF0000, decoded.getRGB(0, 0));
            assertEquals(0xFF00FF00, decoded.getRGB(1, 1));
            assertEquals(0xFF0000FF, decoded.getRGB(2, 2));
            assertEquals(0x00000000, decoded.getRGB(3, 3)); // 透明像素还原
        }
    }

    @Test
    void testVersion6MultiPaletteRoundTrip(@TempDir Path tempDir) throws IOException {
        // V6 多调色板：锁步创建 3 套配色方案→写出重读→逐调色板还原 + 切换激活调色板解码
        Version6Img img = new Version6Img();
        img.setName("test/multipalette.img");

        BufferedImage p0 = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        BufferedImage p1 = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        BufferedImage p2 = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        // 参考图 p0：上行红、下行蓝（两个共享索引分区）
        p0.setRGB(0, 0, 0xFFFF0000);
        p0.setRGB(1, 0, 0xFFFF0000);
        p0.setRGB(0, 1, 0xFF0000FF);
        p0.setRGB(1, 1, 0xFF0000FF);
        // 配色方案 p1：上行绿、下行黄
        p1.setRGB(0, 0, 0xFF00FF00);
        p1.setRGB(1, 0, 0xFF00FF00);
        p1.setRGB(0, 1, 0xFFFFFF00);
        p1.setRGB(1, 1, 0xFFFFFF00);
        // 配色方案 p2：上行白、下行黑
        p2.setRGB(0, 0, 0xFFFFFFFF);
        p2.setRGB(1, 0, 0xFFFFFFFF);
        p2.setRGB(0, 1, 0xFF000000);
        p2.setRGB(1, 1, 0xFF000000);

        img.addPalettesImage(new BufferedImage[]{p0, p1, p2});
        assertEquals(3, img.getPaletteSourceCount());
        assertEquals(1, img.getImageCount());

        NpkFile npk = NpkFile.create();
        npk.addResource(img);
        Path out = tempDir.resolve("v6_multipalette.npk");
        npk.save(out.toFile());

        try (NpkFile reopened = NpkFile.open(out.toFile())) {
            Img loaded = reopened.getImages().get(0);
            assertEquals("test/multipalette.img", loaded.getName());
            assertEquals(6, loaded.getVersion());
            assertEquals(3, loaded.getPaletteSourceCount());
            // 逐调色板颜色还原
            assertEquals(0xFFFF0000, loaded.getPaletteSource(0).get(0).getRGB());
            assertEquals(0xFF0000FF, loaded.getPaletteSource(0).get(1).getRGB());
            assertEquals(0xFF00FF00, loaded.getPaletteSource(1).get(0).getRGB());
            assertEquals(0xFFFFFF00, loaded.getPaletteSource(1).get(1).getRGB());
            assertEquals(0xFFFFFFFF, loaded.getPaletteSource(2).get(0).getRGB());
            assertEquals(0xFF000000, loaded.getPaletteSource(2).get(1).getRGB());
            // 切换激活调色板，同一帧索引解码出不同配色
            assertEquals(0, loaded.getActivePaletteSourceIndex());
            assertEquals(0xFFFF0000, loaded.getImage(0).getRGB(0, 0)); // 调色板0：红
            loaded.activatePaletteSource(1);
            assertEquals(1, loaded.getActivePaletteSourceIndex());
            assertEquals(0xFF00FF00, loaded.getImage(0).getRGB(0, 0)); // 调色板1：绿
            assertEquals(0xFFFFFF00, loaded.getImage(0).getRGB(0, 1)); // 调色板1：黄
            loaded.activatePaletteSource(2);
            assertEquals(0xFFFFFFFF, loaded.getImage(0).getRGB(0, 0)); // 调色板2：白
        }
    }

    @Test
    void testVersion5DdsStructuralRoundTrip(@TempDir Path tempDir) throws IOException {
        // V5 DDS 纹理表 + FXT 帧的结构往返（不依赖 DDS 解码插件，仅验证序列化保真）
        Version5Img img = new Version5Img();
        img.setName("test/dds.img");
        byte[] ddsBytes = new byte[]{0x44, 0x44, 0x53, 0x20, 0x7C, 0x00, 0x00, 0x00};
        int ddsIndex = img.addDdsSource(new DdsSource(ddsBytes, Frame.TYPE_FXT1, 8, 8));
        assertEquals(0, ddsIndex);
        img.addDdsImage(ddsIndex, 0, 0, 4, 4, 2, 3, 4, 4);
        assertEquals(1, img.getImageCount());
        assertEquals(1, img.getDdsSourceCount());

        NpkFile npk = NpkFile.create();
        npk.addResource(img);
        Path out = tempDir.resolve("v5_dds.npk");
        npk.save(out.toFile());

        try (NpkFile reopened = NpkFile.open(out.toFile())) {
            Img loaded = reopened.getImages().get(0);
            assertEquals("test/dds.img", loaded.getName());
            assertEquals(5, loaded.getVersion());
            assertEquals(1, loaded.getDdsSourceCount());
            assertEquals(1, loaded.getImageCount());
            DdsSource dds = loaded.getDdsSource(0);
            assertNotNull(dds);
            assertEquals(Frame.TYPE_FXT1, dds.getPixelFormat());
            assertEquals(8, dds.getWidth());
            assertEquals(8, dds.getHeight());
            assertArrayEquals(ddsBytes, dds.getRawData()); // ZLIB 压缩→解压字节保真
        }
    }

    @Test
    void testAddPalettesImageAtomicity() {
        // V6 多调色板两阶段提交：冲突失败后调色板不得被部分污染，修复后可重试
        Version6Img img = new Version6Img();

        // 第一帧：建立 2 个调色板（各 2 色：红/蓝 与 绿/黄，共享索引分区）
        BufferedImage f1a = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        BufferedImage f1b = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        f1a.setRGB(0, 0, 0xFFFF0000);
        f1a.setRGB(1, 0, 0xFFFF0000);
        f1a.setRGB(0, 1, 0xFF0000FF);
        f1a.setRGB(1, 1, 0xFF0000FF);
        f1b.setRGB(0, 0, 0xFF00FF00);
        f1b.setRGB(1, 0, 0xFF00FF00);
        f1b.setRGB(0, 1, 0xFFFFFF00);
        f1b.setRGB(1, 1, 0xFFFFFF00);
        img.addPalettesImage(new BufferedImage[]{f1a, f1b});
        assertEquals(2, img.getPaletteSource(0).size());
        assertEquals(2, img.getPaletteSource(1).size());

        // 冲突尝试：参考图新增同分区两个像素（白/白），第二图同分区异色（白/黑）→ 抛异常
        BufferedImage f2a = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        BufferedImage f2b = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        f2a.setRGB(0, 0, 0xFFFF0000); // 既有索引 0（红）
        f2a.setRGB(1, 0, 0xFF0000FF); // 既有索引 1（蓝）
        f2a.setRGB(0, 1, 0xFFFFFFFF); // 新增索引 2
        f2a.setRGB(1, 1, 0xFFFFFFFF); // 新增索引 2
        f2b.setRGB(0, 0, 0xFF00FF00); // 索引 0 → 绿 ✓
        f2b.setRGB(1, 0, 0xFFFFFF00); // 索引 1 → 黄 ✓
        f2b.setRGB(0, 1, 0xFFFFFFFF); // 新分区首个像素
        f2b.setRGB(1, 1, 0xFF000000); // 同分区异色 → 冲突
        assertThrows(NpkException.class,
                () -> img.addPalettesImage(new BufferedImage[]{f2a, f2b}));

        // 原子性：失败后调色板仍等长（2/2，未被部分污染），帧数不变
        assertEquals(2, img.getPaletteSource(0).size());
        assertEquals(2, img.getPaletteSource(1).size());
        assertEquals(1, img.getImageCount());

        // 修正后重试成功（旧实现会因调色板尺寸失配被永久锁死）
        f2b.setRGB(1, 1, 0xFFFFFFFF);
        img.addPalettesImage(new BufferedImage[]{f2a, f2b});
        assertEquals(3, img.getPaletteSource(0).size());
        assertEquals(3, img.getPaletteSource(1).size());
        assertEquals(2, img.getImageCount());
    }

    @Test
    void testIndexedEncodeAtomicity() {
        // V4 索引色编码两阶段提交：调色板溢出时不得被部分污染
        Version4Img img = new Version4Img();
        PaletteSource palette = img.getPaletteSource(0);
        for (int i = 0; i < 255; i++) {
            palette.add(new Color(i, i, i));
        }

        // 图像含 2 个新颜色 → 溢出抛出，调色板应保持 255
        BufferedImage image = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xFF123456);
        image.setRGB(1, 0, 0xFF654321);
        assertThrows(NpkException.class, () -> img.addImage(image, Frame.TYPE_INDEXED));
        assertEquals(255, palette.size());
        assertEquals(0, img.getImageCount());

        // 仅 1 个新颜色时可正常提交至 256
        image.setRGB(1, 0, 0xFF123456);
        img.addImage(image, Frame.TYPE_INDEXED);
        assertEquals(256, palette.size());
        assertEquals(1, img.getImageCount());
    }

    @Test
    void testClosedNpkFileRejected(@TempDir Path tempDir) throws IOException {
        // close 后所有操作拒绝，防止 save 用空资源列表覆盖源文件
        NpkFile npk = NpkFile.create();
        npk.addResource(new Ogg("sound/a.ogg", new byte[]{1, 2, 3}));
        Path out = tempDir.resolve("closed.npk");
        npk.save(out.toFile());

        npk.close();
        assertThrows(NpkException.class, () -> npk.addResource(new Ogg("b.ogg", new byte[]{1})));
        assertThrows(NpkException.class, npk::getResources);
        assertThrows(NpkException.class, npk::getResourceCount);
        assertThrows(NpkException.class, () -> npk.save(out.toFile()));

        // 源文件未被空内容覆盖，仍可正常打开
        try (NpkFile reopened = NpkFile.open(out.toFile())) {
            assertEquals(1, reopened.getResourceCount());
        }
    }

    @Test
    void testAddResourceNameValidation() {
        // 超长名称与无法以 ISO-8859-1 无损编码的名称（会被静默截断/替换为 '?'）均提前拒绝
        NpkFile npk = NpkFile.create();
        assertThrows(NpkException.class, () -> npk.addResource(null));
        assertThrows(NpkException.class,
                () -> npk.addResource(new Ogg(repeat('a', 300), new byte[]{1})));
        assertThrows(NpkException.class,
                () -> npk.addResource(new Ogg("sprite/中文名.img", new byte[]{1})));
        assertEquals(0, npk.getResourceCount());
    }

    @Test
    void testCorruptNpkRejected(@TempDir Path tempDir) throws IOException {
        // 魔数有效但 resourceCount 声称 1000，远超文件实际大小 → 拒绝而非 OOM
        byte[] corrupt = new byte[NpkFile.NPK_MAGIC.length + 4 + 32];
        System.arraycopy(NpkFile.NPK_MAGIC, 0, corrupt, 0, NpkFile.NPK_MAGIC.length);
        corrupt[16] = (byte) 0xE8; // 1000 (LE)
        corrupt[17] = 0x03;
        Path out = tempDir.resolve("corrupt.npk");
        Files.write(out, corrupt);
        NpkException e = assertThrows(NpkException.class, () -> NpkFile.open(out.toFile()));
        assertTrue(e.getMessage().contains("Corrupt NPK"));
    }

    @Test
    void testV5ImgLengthBackfill() throws IOException {
        // V5 头部 imgLength 字段（偏移 36）须在写出完成后回填为实际文件大小
        Version5Img img = new Version5Img();
        int ddsIndex = img.addDdsSource(
                new DdsSource(new byte[]{0x44, 0x44, 0x53, 0x20, 0x7C, 0, 0, 0}, Frame.TYPE_FXT1, 8, 8));
        img.addDdsImage(ddsIndex, 0, 0, 4, 4, 0, 0, 4, 4);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream out = new MemoryCacheImageOutputStream(baos)) {
            out.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            img.write(out);
            out.flush();
        }
        byte[] bytes = baos.toByteArray();
        int imgLength = (bytes[36] & 0xFF) | ((bytes[37] & 0xFF) << 8)
                | ((bytes[38] & 0xFF) << 16) | ((bytes[39] & 0xFF) << 24);
        assertEquals(bytes.length, imgLength);
    }

    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
