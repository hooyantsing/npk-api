package npkapi;

import org.junit.jupiter.api.Test;
import xyz.hooy.npkapi.Img;
import xyz.hooy.npkapi.Npk;
import xyz.hooy.npkapi.impl.DefaultNpk;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class NpkApiTest {

    private final static String PATH = "D:/Project/NpkApi/test";

    @Test
    void read() throws IOException {
        File file = new File(PATH + "/output/sprite_map_npc_chn_knight.NPK");
        try (FileImageInputStream stream = new FileImageInputStream(file)) {
            Npk npk = new DefaultNpk();
            npk.read(stream);
            for (int i = 0; i < npk.getTextureSize(); i++) {
                Img img = npk.getImg(i);
                for (int j = 0; j < img.getFrameSize(); j++) {
                    BufferedImage image = img.getImage(j);
                    String imgName = img.getName();
                    imgName = imgName.substring(0, imgName.lastIndexOf("."));
                    imgName = imgName.replace("/", "_") + "_" + j + ".png";
                    ImageIO.write(image, "PNG", new File(PATH + "/output/" + imgName));
                }
            }
        }
    }

    @Test
    void write() throws IOException {
        File readFile = new File(PATH + "/input/sprite_map_npc_chn_knight.NPK");
        File writeFile = new File(PATH + "/output/sprite_map_npc_chn_knight.NPK");
        try (FileImageInputStream inputStream = new FileImageInputStream(readFile); FileImageOutputStream outputStream = new FileImageOutputStream(writeFile) ) {
            Npk npk = new DefaultNpk();
            npk.read(inputStream);
            Img img = npk.getImg(0);
            img.addFrame(2,1);
            BufferedImage frame = ImageIO.read(new File(PATH + "/input/frame.png"));
            img.addFrame(img.getFrameSize(), frame);
            npk.write(outputStream);
        }
    }
}
