package npkapi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.hooy.npkit.*;
import xyz.hooy.npkit.Frame;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NPK 文件读写集成测试。
 * <p>
 * 注意：read/write 测试需要实际的 NPK 文件，标记为手动测试。
 * 单元测试部分验证核心组件逻辑。
 * </p>
 */
public class NpkApiTest {

    /**
     * 手动测试路径 — 仅在有测试资源文件时可用
     */
    private static final String TEST_NPK_PATH = "D:/Project/NpkApi/test/output/sprite_map_npc_chn_knight.NPK";

    @Test
    void testReadNpkFile() throws IOException {
        File file = new File(TEST_NPK_PATH);
        if (!file.exists()) {
            System.out.println("[SKIP] Test NPK file not found: " + TEST_NPK_PATH);
            return;
        }

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
        File file = new File(TEST_NPK_PATH);
        if (!file.exists()) {
            System.out.println("[SKIP] Test NPK file not found: " + TEST_NPK_PATH);
            return;
        }

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
        Img v2 = ImgFactory.create(2);
        assertEquals(2, v2.version());
        assertInstanceOf(Version2Img.class, v2);

        Img v4 = ImgFactory.create(4);
        assertEquals(4, v4.version());
        assertInstanceOf(Version4Img.class, v4);

        Img v5 = ImgFactory.create(5);
        assertEquals(5, v5.version());
        assertInstanceOf(Version5Img.class, v5);

        Img v6 = ImgFactory.create(6);
        assertEquals(6, v6.version());
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
    void testPalette() {
        Palette palette = new Palette();
        palette.add(new Color(255, 0, 0, 255));
        palette.add(new Color(0, 255, 0, 255));
        palette.add(new Color(0, 0, 255, 255));

        assertEquals(3, palette.size());
        assertEquals(0, palette.indexOf(new Color(255, 0, 0, 255)));
        assertEquals(2, palette.indexOf(new Color(0, 0, 255, 255)));
        assertEquals(-1, palette.indexOf(new Color(128, 128, 128, 255)));
        assertTrue(palette.contains(new Color(0, 255, 0, 255)));

        // 测试序列化/反序列化
        byte[] bytes = palette.toBytes();
        assertEquals(12, bytes.length); // 3 colors * 4 bytes
        Palette restored = Palette.fromBytes(bytes, 3);
        assertEquals(palette.size(), restored.size());
        assertEquals(palette.get(0).getRGB(), restored.get(0).getRGB());
    }

    @Test
    void testBytesCompression() {
        byte[] original = "Hello, NPK world! This is a test string for compression.".getBytes();
        byte[] compressed = Bytes.compress(original);
        byte[] decompressed = Bytes.decompress(compressed);
        assertArrayEquals(original, decompressed);
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
        img.addReference(0);
        assertEquals(2, img.getImageCount());
        assertNotNull(img.getImage(1)); // 引用帧应解引用到帧 0
    }

    @Test
    void testOggResource() {
        byte[] fakeAudioData = new byte[]{0x4F, 0x67, 0x67, 0x53}; // "OggS" magic
        Ogg ogg = new Ogg("sound/bgm/test.ogg", fakeAudioData);

        assertEquals("sound/bgm/test.ogg", ogg.getName());
        assertEquals(NpkResource.Type.OGG, ogg.getType());
        assertEquals(4, ogg.getSize());
        assertArrayEquals(fakeAudioData, ogg.getRawData());
    }

    @Test
    void testNpkFileCreate() throws IOException {
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
}
