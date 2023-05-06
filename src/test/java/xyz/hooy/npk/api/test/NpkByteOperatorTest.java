package xyz.hooy.npk.api.test;

import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.Test;
import xyz.hooy.npk.api.operation.NpkByteOperator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Ignore
public class NpkByteOperatorTest {

    private static final String INPUT_PATH = "/Users/hooy/Project/npk-loader/input/";
    private static final String OUTPUT_PATH = "/Users/hooy/Project/npk-api/output/";
    private static final String INPUT_NPK_FILE = INPUT_PATH + "sprite_map_npc_chn_knight.NPK";

    @Test
    void add() throws IOException {
        byte[] img = Files.readAllBytes(Paths.get(INPUT_PATH + "modred.img"));
        NpkByteOperator npkByteOperator = new NpkByteOperator(Paths.get(INPUT_PATH + "sprite_common.NPK"));
        npkByteOperator.add(img, "img/hooy/modred.img");
        byte[] build = npkByteOperator.build();
        Files.write(Paths.get(OUTPUT_PATH + "NpkByteOperator-add-test.npk"), build);
    }

    @Test
    void remove() throws IOException {
        byte[] npk = Files.readAllBytes(Paths.get(INPUT_NPK_FILE));
        NpkByteOperator npkByteOperator = new NpkByteOperator(npk);
        npkByteOperator.remove(4);
        byte[] build = npkByteOperator.build();
        Files.write(Paths.get(OUTPUT_PATH + "NpkByteOperator-remove-test.npk"), build);
    }

    @Test
    void rename() throws IOException {
        byte[] npk = Files.readAllBytes(Paths.get(INPUT_NPK_FILE));
        NpkByteOperator npkByteOperator = new NpkByteOperator(npk);
        npkByteOperator.rename(2, "img/hooy/rename.img");
        byte[] build = npkByteOperator.build();
        Files.write(Paths.get(OUTPUT_PATH + "NpkByteOperator-rename-test.npk"), build);
    }
}
