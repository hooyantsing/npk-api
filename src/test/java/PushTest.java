import xyz.hooy.npk.api.NpkByteOperator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

public class PushTest {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        byte[] bytes = Files.readAllBytes(Paths.get("/Users/hooy/Project/npk-loader/input/modred.img"));
        NpkByteOperator npkByteOperator = new NpkByteOperator(Paths.get("/Users/hooy/Project/npk-loader/input/sprite_map_act2_stoneimage.NPK"));
        byte[] build = npkByteOperator.add(bytes, "hooy/img/modred.img").build();
        Files.write(Paths.get("/Users/hooy/Project/npk-loader/output/test.npk"), build);
    }
}
