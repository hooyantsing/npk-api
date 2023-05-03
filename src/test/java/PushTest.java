import xyz.hooy.npk.api.NpkByteReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

public class PushTest {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        byte[] bytes = Files.readAllBytes(Paths.get("/Users/hooy/Project/npk-loader/input/modred.img"));
        NpkByteReader npkByteReader = new NpkByteReader(Paths.get("/Users/hooy/Project/npk-loader/input/sprite_map_act2_stoneimage.NPK"));
        byte[] build = npkByteReader.push(bytes, "hooy/img/modred.img").build();
        Files.write(Paths.get("/Users/hooy/Project/npk-loader/output/test.npk"), build);
    }
}
