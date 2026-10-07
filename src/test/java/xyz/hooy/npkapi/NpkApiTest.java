package xyz.hooy.npkapi;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.hooy.npkapi.codec.Argb8888Codec;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 核心组件单元测试与（可选的）真实 NPK 文件集成测试。
 * <p>
 * 集成测试需要真实的游戏 NPK 文件，通过系统属性 {@code npk.test.file} 指定路径：
 * <pre>mvn test -Dnpk.test.file=/path/to/sprite_map_npc_chn_knight.NPK</pre>
 * 未指定或文件不存在时以 JUnit Assumption 显式跳过（而非静默通过）。
 * </p>
 */
public class NpkApiTest {

    /**
     * 手动集成测试的 NPK 文件路径系统属性
     */
    private static final String TEST_NPK_PROPERTY = "npk.test.file";

    private static File manualTestNpk() {
        String path = System.getProperty(TEST_NPK_PROPERTY);
        File file = path != null ? new File(path) : null;
        Assumptions.assumeTrue(file != null && file.isFile(),
                "Manual integration test skipped: set -D" + TEST_NPK_PROPERTY + "=/path/to/file.NPK");
        return file;
    }

    @Test
    void testReadNpkFile() throws IOException {
        File file = manualTestNpk();
        try (NpkFile npk = NpkFile.open(file)) {
            assertTrue(npk.getResourceCount() > 0, "NPK should contain at least one resource");

            for (Img img : npk.getImages()) {
                assertNotNull(img.getName());
                assertTrue(img.getImageCount() > 0);

                // 验证每帧都能解码
                for (int j = 0; j < img.getImageCount(); j++) {
                    BufferedImage image = img.getImage(j);
                    assertNotNull(image, "Frame " + j + " should decode to an image");
                }
            }
        }
    }

    @Test
    void testExportFramesAsPng(@TempDir Path tempDir) throws IOException {
        File file = manualTestNpk();
        try (NpkFile npk = NpkFile.open(file)) {
            for (Img img : npk.getImages()) {
                String imgName = img.getName();
                imgName = imgName.substring(0, imgName.lastIndexOf("."));
                imgName = imgName.replace("/", "_");

                for (int j = 0; j < img.getImageCount(); j++) {
                    BufferedImage image = img.getImage(j);
                    File output = tempDir.resolve(imgName + "_" + j + ".png").toFile();
                    ImageIO.write(image, "PNG", output);
                    assertTrue(output.exists());
                }
            }
        }
    }

    @Test
    void testImgFactory() {
        Img v1 = ImgFactory.create(1);
        assertEquals(1, v1.getVersion());
        assertInstanceOf(Version1Img.class, v1);

        Img v2 = ImgFactory.create(2);
        assertEquals(2, v2.getVersion());
        assertInstanceOf(Version2Img.class, v2);

        Img v3 = ImgFactory.create(3);
        assertEquals(3, v3.getVersion());
        assertInstanceOf(Version3Img.class, v3);

        Img v4 = ImgFactory.create(4);
        assertEquals(4, v4.getVersion());
        assertInstanceOf(Version4Img.class, v4);

        Img v5 = ImgFactory.create(5);
        assertEquals(5, v5.getVersion());
        assertInstanceOf(Version5Img.class, v5);

        Img v6 = ImgFactory.create(6);
        assertEquals(6, v6.getVersion());
        assertInstanceOf(Version6Img.class, v6);

        assertTrue(ImgFactory.isSupported(2));
        assertFalse(ImgFactory.isSupported(99));

        assertThrows(NpkException.class, () -> ImgFactory.create(99));
    }

    @Test
    void testFrameTypeChecks() {
        Frame argbFrame = new Frame(Frame.TYPE_ARGB8888);
        assertTrue(argbFrame.isArgb());
        assertFalse(argbFrame.isReference());
        assertFalse(argbFrame.isFxt());

        Frame refFrame = new Frame(Frame.TYPE_REFERENCE);
        assertTrue(refFrame.isReference());
        assertFalse(refFrame.isArgb());

        Frame fxtFrame = new Frame(Frame.TYPE_FXT1);
        assertTrue(fxtFrame.isFxt());
        assertFalse(fxtFrame.isArgb());
    }

    @Test
    void testFrameLengthDerivedFromData() {
        // length 单一事实来源：挂载数据后以数据长度为准，setLength 的残留值被忽略
        Frame frame = new Frame(Frame.TYPE_ARGB8888);
        frame.setLength(999);
        assertEquals(999, frame.getLength()); // 无数据时返回描述值（FXT 保留字段语义）
        frame.setRawData(new byte[16]);
        assertEquals(16, frame.getLength());
    }

    @Test
    void testPalette() {
        PaletteSource palette = new PaletteSource();
        palette.add(new Color(255, 0, 0, 255));
        palette.add(new Color(0, 255, 0, 255));
        palette.add(new Color(0, 0, 255, 255));

        assertEquals(3, palette.size());
        assertEquals(0, palette.indexOf(new Color(255, 0, 0, 255)));
        assertEquals(2, palette.indexOf(new Color(0, 0, 255, 255)));
        assertEquals(-1, palette.indexOf(new Color(128, 128, 128, 255)));
        assertTrue(palette.indexOf(new Color(0, 255, 0, 255)) >= 0);

        // 测试序列化/反序列化
        byte[] bytes = palette.toBytes();
        assertEquals(12, bytes.length); // 3 colors * 4 bytes
        PaletteSource restored = PaletteSource.fromBytes(bytes, 3);
        assertEquals(palette.size(), restored.size());
        assertEquals(palette.get(0).getRGB(), restored.get(0).getRGB());
    }

    @Test
    void testBytesCompression() {
        byte[] original = "Hello, NPK world! This is a test string for compression.".getBytes();
        byte[] compressed = Bytes.compress(original);
        byte[] decompressed = Bytes.decompress(compressed, original.length);
        assertArrayEquals(original, decompressed);
    }

    @Test
    void testDecompressLimitEnforced() {
        // 解压上限防御：声明大小小于实际数据时拒绝（压缩炸弹/损坏文件防御）
        byte[] original = new byte[4096];
        byte[] compressed = Bytes.compress(original);
        assertThrows(NpkException.class, () -> Bytes.decompress(compressed, 1024));

        // 损坏数据拒绝而非挂起/静默
        byte[] garbage = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        assertThrows(NpkException.class, () -> Bytes.decompress(garbage, 4096));
    }

    @Test
    void testVersion2ImgAddImage() {
        Version2Img img = new Version2Img();
        img.setName("test/img/sample.img");

        BufferedImage testImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        img.addImage(testImage, Frame.TYPE_ARGB8888);

        assertEquals(1, img.getImageCount());
        assertEquals("test/img/sample.img", img.getName());
        assertEquals(NpkResource.Type.IMG, img.getType());

        // 添加引用帧
        img.addReferenceImage(0);
        assertEquals(2, img.getImageCount());
        assertNotNull(img.getImage(1)); // 引用帧应解引用到帧 0

        // 帧元数据访问
        Frame frame0 = img.getFrame(0);
        assertEquals(16, frame0.getWidth());
        assertEquals(16, frame0.getHeight());
        assertTrue(img.getFrame(1).isReference());
        assertThrows(IndexOutOfBoundsException.class, () -> img.getFrame(2));
    }

    @Test
    void testReferenceChainFlattened() {
        // 引用链展平：指向引用帧时自动解析到最终图像帧（深度恒为 1，
        // 规避 DNF 客户端不检测有向环、深链闪退的兼容性问题）
        Version2Img img = new Version2Img();
        img.addImage(new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB), Frame.TYPE_ARGB8888);
        img.addReferenceImage(0); // 帧1 -> 帧0
        img.addReferenceImage(1); // 帧2 -> 展平为 帧0
        img.addReferenceImage(2); // 帧3 -> 展平为 帧0

        assertEquals(0, img.getFrame(1).getReference());
        assertEquals(0, img.getFrame(2).getReference());
        assertEquals(0, img.getFrame(3).getReference());
        assertNotNull(img.getImage(3));
    }

    @Test
    void testDecodedImageIsIndependentFromFrameData() {
        // 回归：解码返回的图像必须与帧内部数据完全独立，
        // 修改解码图像的像素不得污染帧数据（旧实现未压缩帧共享数组）
        Argb8888Codec codec = new Argb8888Codec();
        Frame frame = new Frame(Frame.TYPE_ARGB8888);
        frame.setCompressed(Frame.UNCOMPRESSED);
        frame.setWidth(4);
        frame.setHeight(4);
        byte[] pixels = new byte[4 * 4 * 4]; // BGRA，(0,0) 设为不透明红
        pixels[2] = (byte) 0xFF;
        pixels[3] = (byte) 0xFF;
        frame.setRawData(pixels);
        byte[] snapshot = pixels.clone();

        BufferedImage decoded = codec.decode(frame, null);
        assertEquals(0xFFFF0000, decoded.getRGB(0, 0));
        decoded.setRGB(1, 1, 0xFF00FF00); // 修改解码图像
        decoded.setRGB(0, 0, 0xFF0000FF);

        assertArrayEquals(snapshot, frame.getRawData(), "Frame data must not be modified");
        BufferedImage redecoded = codec.decode(frame, null);
        assertEquals(0xFFFF0000, redecoded.getRGB(0, 0));
        assertEquals(0x00000000, redecoded.getRGB(1, 1));
    }

    @Test
    void testOggResource() {
        byte[] fakeAudioData = new byte[]{0x4F, 0x67, 0x67, 0x53}; // "OggS" magic
        Ogg ogg = new Ogg("sound/bgm/test.ogg", fakeAudioData);

        assertEquals("sound/bgm/test.ogg", ogg.getName());
        assertEquals(NpkResource.Type.OGG, ogg.getType());
        assertEquals(4, ogg.getSize());
        assertArrayEquals(fakeAudioData, ogg.getData());
    }

    @Test
    void testNpkFileCreate() {
        NpkFile npk = NpkFile.create();
        assertEquals(0, npk.getResourceCount());

        Version2Img img = new Version2Img();
        img.setName("test/sample.img");
        BufferedImage testImage = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        img.addImage(testImage, Frame.TYPE_ARGB8888);

        npk.addResource(img);
        npk.addResource(new Ogg("test/audio.ogg", new byte[100]));

        assertEquals(2, npk.getResourceCount());
        assertEquals(1, npk.getImages().size());
        assertEquals(1, npk.getOggs().size());
        assertNotNull(npk.findByName("test/sample.img"));
        assertNull(npk.findByName("nonexist.img"));
    }

    @Test
    void testResourceNameEncryptDecryptRoundTrip() {
        // 资源名 XOR 加解密往返（含边界：255 字节最长名、空名）
        String[] names = {
                "character/swordman/effect.img",
                "sound/bgm/stage1.ogg",
                repeat('a', 255),
                ""
        };
        for (String name : names) {
            byte[] encrypted = NpkFile.encryptName(name);
            assertEquals(256, encrypted.length);
            assertEquals(name, NpkFile.decryptName(encrypted));
        }
    }

    @Test
    void testDdsTableRejectsDuplicateIndex() {
        DdsTable table = new DdsTable();
        DdsSource first = new DdsSource(new byte[]{1}, Frame.TYPE_FXT1, 4, 4);
        table.put(first); // 自动分配索引 0
        assertEquals(0, first.getIndex());

        DdsSource conflicting = new DdsSource(new byte[]{2}, Frame.TYPE_FXT1, 4, 4);
        conflicting.setIndex(0);
        assertThrows(NpkException.class, () -> table.put(conflicting));
        assertEquals(1, table.size());
        assertSame(first, table.get(0));
    }

    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
