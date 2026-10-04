package npkapi;

import org.junit.jupiter.api.Test;
import xyz.hooy.npkit.*;
import xyz.hooy.npkit.codec.Argb8888Codec;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NpkFile 写入功能测试。
 */
public class NpkitTest {

    @Test
    void testNpkFileCreateAndWrite(@org.junit.jupiter.api.io.TempDir Path tempDir) throws IOException {
        // 构建 IMG 资源
        Version2Img img = new Version2Img();
        img.setName("character/swordman/effect.img");
        BufferedImage testImage = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        img.addImage(testImage, Frame.TYPE_ARGB8888);
        img.addImage(testImage, Frame.TYPE_ARGB1555);
        img.addReference(0);

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
    void testNpkFileSaveAndReopen(@org.junit.jupiter.api.io.TempDir Path tempDir) throws IOException {
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

        // 验证 V5 注册了 DDS 编解码器
        Version5Img v5 = new Version5Img();
        assertEquals(5, v5.version());
        assertNull(v5.getDdsTable()); // 未读取数据前为 null
        assertEquals(0, v5.getDdsSourceCount());
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
}
