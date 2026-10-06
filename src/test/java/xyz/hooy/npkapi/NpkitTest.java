package xyz.hooy.npkapi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.hooy.npkapi.codec.Argb8888Codec;

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
 * NpkFile 写入与往返（round-trip）保真测试。
 * <p>
 * 资源以内存模型为单一事实来源：所有保存路径均从模型重新序列化，
 * 本类重点验证「保存 → 重开 → 内容还原」以及旧 rawData 缓存实现
 * 曾丢失修改的回归场景。
 * </p>
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
        // 创建并保存（V4 索引色 + OGG），重开后逐资源验证内容还原
        NpkFile npk = NpkFile.create();
        Version4Img img = new Version4Img();
        img.setName("equip/weapon.img");

        BufferedImage testImage = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        testImage.setRGB(3, 4, 0xFFFF0000);
        img.addImage(testImage, Frame.TYPE_INDEXED);

        byte[] oggData = new byte[]{0x4F, 0x67, 0x67, 0x53, 1, 2, 3};
        npk.addResource(img);
        npk.addResource(new Ogg("bgm/stage1.ogg", oggData));

        Path outputPath = tempDir.resolve("saved.npk");
        npk.save(outputPath.toFile());
        assertTrue(Files.exists(outputPath));

        try (NpkFile reopened = NpkFile.open(outputPath.toFile())) {
            assertEquals(2, reopened.getResourceCount());
            assertEquals(outputPath.toFile(), reopened.getSourceFile());

            Img loadedImg = reopened.getImages().get(0);
            assertEquals("equip/weapon.img", loadedImg.getName());
            assertEquals(4, loadedImg.getVersion());
            assertEquals(1, loadedImg.getImageCount());
            assertEquals(0xFFFF0000, loadedImg.getImage(0).getRGB(3, 4));

            Ogg loadedOgg = reopened.getOggs().get(0);
            assertEquals("bgm/stage1.ogg", loadedOgg.getName());
            assertArrayEquals(oggData, loadedOgg.getData());
        }
    }

    @Test
    void testSaveToSourceFile(@TempDir Path tempDir) throws IOException {
        // save() 便捷方法：保存回源文件；无源文件时明确拒绝
        NpkFile fresh = NpkFile.create();
        assertThrows(NpkException.class, fresh::save);

        NpkFile npk = NpkFile.create();
        npk.addResource(new Ogg("bgm/a.ogg", new byte[]{1, 2, 3}));
        Path out = tempDir.resolve("source.npk");
        npk.save(out.toFile());
        npk.addResource(new Ogg("bgm/b.ogg", new byte[]{4, 5, 6}));
        npk.save(); // 覆盖源文件

        try (NpkFile reopened = NpkFile.open(out.toFile())) {
            assertEquals(2, reopened.getResourceCount());
        }
    }

    @Test
    void testDirectPaletteMutationIsSaved(@TempDir Path tempDir) throws IOException {
        // 回归：旧实现缓存资源原始字节，重开文件后直接修改 PaletteSource
        // 不会使缓存失效，save 时修改被静默丢弃；模型单一事实来源后必须如实写出
        Version4Img img = new Version4Img();
        img.setName("test/palette_mutation.img");
        BufferedImage red = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        red.setRGB(0, 0, 0xFFFF0000);
        img.addImage(red, Frame.TYPE_INDEXED);

        NpkFile npk = NpkFile.create();
        npk.addResource(img);
        Path out = tempDir.resolve("mutation.npk");
        npk.save(out.toFile());
        npk.close();

        try (NpkFile reopened = NpkFile.open(out.toFile())) {
            PaletteCapable loaded = assertInstanceOf(PaletteCapable.class,
                    reopened.getImages().get(0));
            assertEquals(1, loaded.getPaletteSource(0).size());
            // 直接修改调色板子对象（不经过任何 Img 级 API）
            loaded.getPaletteSource(0).add(new Color(0, 255, 0, 255));
            reopened.save(out.toFile());
        }

        try (NpkFile verify = NpkFile.open(out.toFile())) {
            PaletteCapable loaded = assertInstanceOf(PaletteCapable.class,
                    verify.getImages().get(0));
            assertEquals(2, loaded.getPaletteSource(0).size());
            assertEquals(0xFF00FF00, loaded.getPaletteSource(0).get(1).getRGB());
        }
    }

    @Test
    void testCapabilityInterfaces() {
        Version2Img v2 = new Version2Img();
        Version4Img v4 = new Version4Img();
        Version5Img v5 = new Version5Img();
        Version6Img v6 = new Version6Img();

        // DDS 能力仅 V5 具备
        assertFalse(v2 instanceof DdsCapable);
        assertFalse(v4 instanceof DdsCapable);
        assertInstanceOf(DdsCapable.class, v5);
        assertFalse(v6 instanceof DdsCapable);

        // 调色板能力 V4/V6 具备；多调色板能力仅 V6 具备
        assertFalse(v2 instanceof PaletteCapable);
        assertInstanceOf(PaletteCapable.class, v4);
        assertFalse(v5 instanceof PaletteCapable);
        assertInstanceOf(PaletteCapable.class, v6);
        assertFalse(v4 instanceof MultiPaletteCapable);
        assertInstanceOf(MultiPaletteCapable.class, v6);

        // V4 单调色板契约
        assertEquals(1, v4.getPaletteSourceCount());
        assertEquals(0, v4.getActivePaletteSourceIndex());
        assertNotNull(v4.getPaletteSource(0));
        assertThrows(IndexOutOfBoundsException.class, () -> v4.activatePaletteSource(1));
        assertThrows(IndexOutOfBoundsException.class, () -> v4.getPaletteSource(1));

        // V5 空 DDS 表契约：越界访问明确抛出而非返回 null
        assertEquals(0, v5.getDdsSourceCount());
        assertThrows(IndexOutOfBoundsException.class, () -> v5.getDdsSource(0));
        assertThrows(NpkException.class, () -> v5.addDdsSource(null));
        assertThrows(NpkException.class,
                () -> v5.addDdsSource(new DdsSource(null, Frame.TYPE_FXT1, 4, 4)));

        // V6 初始单调色板
        assertEquals(1, v6.getPaletteSourceCount());
        assertEquals(0, v6.getActivePaletteSourceIndex());
        assertThrows(IndexOutOfBoundsException.class, () -> v6.activatePaletteSource(1));
    }

    @Test
    void testCustomVersionRegistration() {
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
        assertEquals(0xFFFF0000, decoded.getRGB(0, 0));
        assertEquals(0xFF00FF00, decoded.getRGB(1, 1));
        assertEquals(0xFF0000FF, decoded.getRGB(2, 2));
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

        img.addMultiPaletteImage(new BufferedImage[]{p0, p1, p2});
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
            PaletteCapable paletted = assertInstanceOf(PaletteCapable.class, loaded);
            assertEquals(3, paletted.getPaletteSourceCount());
            // 逐调色板颜色还原
            assertEquals(0xFFFF0000, paletted.getPaletteSource(0).get(0).getRGB());
            assertEquals(0xFF0000FF, paletted.getPaletteSource(0).get(1).getRGB());
            assertEquals(0xFF00FF00, paletted.getPaletteSource(1).get(0).getRGB());
            assertEquals(0xFFFFFF00, paletted.getPaletteSource(1).get(1).getRGB());
            assertEquals(0xFFFFFFFF, paletted.getPaletteSource(2).get(0).getRGB());
            assertEquals(0xFF000000, paletted.getPaletteSource(2).get(1).getRGB());
            // 切换激活调色板，同一帧索引解码出不同配色
            assertEquals(0, paletted.getActivePaletteSourceIndex());
            assertEquals(0xFFFF0000, paletted.getImage(0).getRGB(0, 0)); // 调色板0：红
            paletted.activatePaletteSource(1);
            assertEquals(1, paletted.getActivePaletteSourceIndex());
            assertEquals(0xFF00FF00, paletted.getImage(0).getRGB(0, 0)); // 调色板1：绿
            assertEquals(0xFFFFFF00, paletted.getImage(0).getRGB(0, 1)); // 调色板1：黄
            paletted.activatePaletteSource(2);
            assertEquals(0xFFFFFFFF, paletted.getImage(0).getRGB(0, 0)); // 调色板2：白
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
        // 裁切 (0,0)-(4,4)，画布 4x4，绘制原点 (2,3)
        img.addDdsImage(ddsIndex, new Rectangle(0, 0, 4, 4), new Rectangle(2, 3, 4, 4));
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
            assertEquals(1, loaded.getImageCount());
            DdsCapable ddsImg = assertInstanceOf(DdsCapable.class, loaded);
            assertEquals(1, ddsImg.getDdsSourceCount());
            DdsSource dds = ddsImg.getDdsSource(0);
            assertEquals(Frame.TYPE_FXT1, dds.getPixelFormat());
            assertEquals(8, dds.getWidth());
            assertEquals(8, dds.getHeight());
            assertEquals(ddsBytes.length, dds.getFullLength());
            assertArrayEquals(ddsBytes, dds.getData()); // ZLIB 压缩→解压字节保真
        }
    }

    @Test
    void testAddDdsImageValidation() {
        Version5Img img = new Version5Img();
        int ddsIndex = img.addDdsSource(
                new DdsSource(new byte[]{0x44, 0x44, 0x53, 0x20}, Frame.TYPE_FXT1, 8, 8));

        // 裁切区域越界 / 非正尺寸 / 画布非正尺寸均拒绝
        assertThrows(NpkException.class,
                () -> img.addDdsImage(ddsIndex, new Rectangle(4, 4, 8, 8), new Rectangle(0, 0, 4, 4)));
        assertThrows(NpkException.class,
                () -> img.addDdsImage(ddsIndex, new Rectangle(0, 0, 0, 4), new Rectangle(0, 0, 4, 4)));
        assertThrows(NpkException.class,
                () -> img.addDdsImage(ddsIndex, new Rectangle(0, 0, 4, 4), new Rectangle(0, 0, 0, 4)));
        // 不存在的 DDS 索引拒绝
        assertThrows(NpkException.class,
                () -> img.addDdsImage(42, new Rectangle(0, 0, 4, 4), new Rectangle(0, 0, 4, 4)));
        // 非 FXT 像素格式拒绝
        int badFormat = img.addDdsSource(
                new DdsSource(new byte[]{1}, Frame.TYPE_ARGB8888, 8, 8));
        assertThrows(NpkException.class,
                () -> img.addDdsImage(badFormat, new Rectangle(0, 0, 4, 4), new Rectangle(0, 0, 4, 4)));
        assertEquals(0, img.getImageCount());
    }

    @Test
    void testV5ImgLengthBackfill() throws IOException {
        // V5 头部 imgLength 字段须在写出完成后回填为实际 IMG 大小
        Version5Img img = newVersion5WithDdsFrame();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream out = new MemoryCacheImageOutputStream(baos)) {
            out.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            img.write(out);
            out.flush();
        }
        byte[] bytes = baos.toByteArray();
        assertEquals(bytes.length, Bytes.readIntLE(bytes, imgLengthFieldOffset()));
    }

    @Test
    void testV5ImgLengthBackfillAtOffset() throws IOException {
        // 回归：imgLength 回填须相对写出起始位置，而非假设流从 0 开始
        Version5Img img = newVersion5WithDdsFrame();
        int prefixLength = 10;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream out = new MemoryCacheImageOutputStream(baos)) {
            out.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            out.write(new byte[prefixLength]); // 模拟带偏移的写出（如嵌入更大容器）
            img.write(out);
            out.flush();
        }
        byte[] bytes = baos.toByteArray();
        int imgLength = Bytes.readIntLE(bytes, prefixLength + imgLengthFieldOffset());
        assertEquals(bytes.length - prefixLength, imgLength);
    }

    @Test
    void testAddMultiPaletteImageAtomicity() {
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
        img.addMultiPaletteImage(new BufferedImage[]{f1a, f1b});
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
                () -> img.addMultiPaletteImage(new BufferedImage[]{f2a, f2b}));

        // 原子性：失败后调色板仍等长（2/2，未被部分污染），帧数不变
        assertEquals(2, img.getPaletteSource(0).size());
        assertEquals(2, img.getPaletteSource(1).size());
        assertEquals(1, img.getImageCount());

        // 修正后重试成功（旧实现会因调色板尺寸失配被永久锁死）
        f2b.setRGB(1, 1, 0xFFFFFFFF);
        img.addMultiPaletteImage(new BufferedImage[]{f2a, f2b});
        assertEquals(3, img.getPaletteSource(0).size());
        assertEquals(3, img.getPaletteSource(1).size());
        assertEquals(2, img.getImageCount());

        // 多调色板状态下 addImage 拒绝（须走锁步入口）
        BufferedImage any = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        assertThrows(NpkException.class, () -> img.addImage(any, Frame.TYPE_INDEXED));
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
        assertThrows(NpkException.class, npk::getSourceFile);
        assertThrows(NpkException.class, () -> npk.save(out.toFile()));
        assertThrows(NpkException.class, npk::save);

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
        assertThrows(NpkException.class,
                () -> npk.addResource(new Ogg("no/data.ogg", null)));
        assertEquals(0, npk.getResourceCount());
    }

    @Test
    void testCorruptNpkRejected(@TempDir Path tempDir) throws IOException {
        // 魔数有效但 resourceCount 声称 1000，远超文件实际大小 → 拒绝而非 OOM
        byte[] magic = NpkFile.getNpkMagic();
        byte[] corrupt = new byte[magic.length + 4 + 32];
        System.arraycopy(magic, 0, corrupt, 0, magic.length);
        corrupt[16] = (byte) 0xE8; // 1000 (LE)
        corrupt[17] = 0x03;
        Path out = tempDir.resolve("corrupt.npk");
        Files.write(out, corrupt);
        NpkException e = assertThrows(NpkException.class, () -> NpkFile.open(out.toFile()));
        assertTrue(e.getMessage().contains("Corrupt NPK"));
    }

    /**
     * 构建一个含 1 张 DDS 纹理与 1 个 FXT 帧的 V5 实例（imgLength 回填测试用）
     */
    private static Version5Img newVersion5WithDdsFrame() {
        Version5Img img = new Version5Img();
        int ddsIndex = img.addDdsSource(
                new DdsSource(new byte[]{0x44, 0x44, 0x53, 0x20, 0x7C, 0, 0, 0}, Frame.TYPE_FXT1, 8, 8));
        img.addDdsImage(ddsIndex, new Rectangle(0, 0, 4, 4), new Rectangle(0, 0, 4, 4));
        return img;
    }

    /**
     * imgLength 字段相对 IMG 起始位置的偏移：
     * version(VERSION_OFFSET) + version(4) + frameCount(4) + ddsCount(4)
     */
    private static int imgLengthFieldOffset() {
        return AbstractImg.VERSION_OFFSET + 4 + 4 + 4;
    }

    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
