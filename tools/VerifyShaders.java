import java.nio.file.*;
import java.util.regex.*;
import java.util.zip.*;
import org.lwjgl.util.shaderc.Shaderc;

/** Run with the client's lwjgl and lwjgl-shaderc jars (including Windows natives) on the classpath.
 * Arguments: generated shader directory, Minecraft 26.3 client jar.
 */
class VerifyShaders {
    static String expand(String source, ZipFile client) throws Exception {
        var matcher = Pattern.compile("#include <minecraft:([^>]+)>").matcher(source);
        var result = new StringBuilder();
        while (matcher.find()) {
            var entry = client.getEntry("assets/minecraft/shaders/include/" + matcher.group(1));
            if (entry == null) throw new IllegalStateException("Missing include: " + matcher.group(1));
            String included;
            try (var input = client.getInputStream(entry)) {
                included = expand(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8), client);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(included));
        }
        return matcher.appendTail(result).toString();
    }

    public static void main(String[] args) throws Exception {
        long compiler = Shaderc.shaderc_compiler_initialize();
        long options = Shaderc.shaderc_compile_options_initialize();
        Shaderc.shaderc_compile_options_set_auto_bind_uniforms(options, true);
        try (var client = new ZipFile(args[1])) {
            int count = 0;
            for (String variant : new String[]{"", "IS_GUI", "IS_SEE_THROUGH"}) {
                for (String oit : new String[]{"", "#define OIT\n#define OIT_ACCUMULATE\n",
                        "#define OIT\n#define OIT_ALPHA_ONLY\n#define OIT_TRANSMITTANCE\n",
                        "#define OIT\n#define OIT_ALPHA_ONLY\n#define OIT_DEPTH_BOUNDS\n"}) {
                    for (boolean grayscale : new boolean[]{false, true}) {
                        for (String extension : new String[]{"vsh", "fsh"}) {
                            String source = expand(Files.readString(Path.of(args[0], "text." + extension)), client);
                            String defines = "#define OIT_COEFF_COUNT 8\n#define OIT_WAVELET_RANK 2\n#define OIT_COEFF_ATTACHMENT_COUNT 2\n"
                                + (variant.isEmpty() ? "" : "#define " + variant + "\n")
                                + oit
                                + (grayscale ? "#define IS_GRAYSCALE\n" : "");
                            source = source.replace("#version 330", "#version 330\n" + defines);
                            long result = Shaderc.shaderc_compile_into_spv(compiler, source,
                                extension.equals("vsh") ? Shaderc.shaderc_vertex_shader : Shaderc.shaderc_fragment_shader,
                                "text." + extension, "main", options);
                            try {
                                if (Shaderc.shaderc_result_get_compilation_status(result) != Shaderc.shaderc_compilation_status_success)
                                    throw new IllegalStateException(variant + " " + oit + " " + extension + "\n"
                                        + Shaderc.shaderc_result_get_error_message(result));
                            } finally { Shaderc.shaderc_result_release(result); }
                            count++;
                        }
                    }
                }
            }
            System.out.println("Compiled " + count + " shader stages against Minecraft 26.3 includes.");
        } finally {
            Shaderc.shaderc_compile_options_release(options);
            Shaderc.shaderc_compiler_release(compiler);
        }
    }
}
