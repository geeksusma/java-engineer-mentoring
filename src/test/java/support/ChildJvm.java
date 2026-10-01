package support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Some JVM behaviour (which GC is active, object header layout, carrier
 * thread count) is fixed at startup by command-line flags, so it can only be
 * observed by launching a fresh JVM with those flags. This helper runs a main
 * class from this project in such a child JVM and captures its output.
 */
public final class ChildJvm {

    public record Result(int exitCode, String output) {

        public String valueOf(String key) {
            return output.lines()
                    .filter(line -> line.startsWith(key + "="))
                    .map(line -> line.substring(key.length() + 1))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no '" + key + "' in output:\n" + output));
        }
    }

    private ChildJvm() {
    }

    public static Result run(Class<?> mainClass, String... jvmOptions) throws IOException, InterruptedException {
        var command = new ArrayList<String>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.addAll(List.of(jvmOptions));
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(mainClass.getName());

        var process = new ProcessBuilder(command).redirectErrorStream(true).start();
        var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("child JVM timed out: " + command);
        }
        return new Result(process.exitValue(), output);
    }
}
