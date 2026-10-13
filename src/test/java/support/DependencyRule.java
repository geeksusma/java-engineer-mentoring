package support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Architecture styles boil down to rules like "classes over here must not
 * reference classes over there". This helper reads the compiled production
 * classes with the Class-File API (final since Java 24, JEP 484) and collects
 * every class each one references: field types, method signatures, generics
 * and calls inside method bodies.
 * <p>
 * In a real project reach for ArchUnit; this keeps the lessons dependency-free.
 */
public final class DependencyRule {

    private static final Pattern TYPE_IN_DESCRIPTOR = Pattern.compile("L([\\w/$]+)[;<]");

    private final String description;
    private final List<String> classes;

    private DependencyRule(String description, List<String> classes) {
        this.description = description;
        this.classes = classes;
    }

    public static DependencyRule classesIn(String... packages) {
        var classes = Stream.of(packages).flatMap(DependencyRule::productionClassesIn).sorted().toList();
        if (classes.isEmpty()) {
            fail("no production classes found in " + List.of(packages) + " — a rule over nothing always passes");
        }
        return new DependencyRule(String.join(", ", packages), classes);
    }

    /**
     * Each entry is a package (sub-packages included) or a fully qualified class name.
     */
    public void shouldNotDependOn(String... forbiddenPackages) {
        var violations = new ArrayList<String>();
        for (var className : classes) {
            for (var dependency : dependenciesOf(className)) {
                for (var forbidden : forbiddenPackages) {
                    if (dependency.equals(forbidden) || dependency.startsWith(forbidden + ".")
                            || dependency.startsWith(forbidden + "$")) {
                        violations.add(className + " -> " + dependency);
                    }
                }
            }
        }
        if (!violations.isEmpty()) {
            fail("classes in [" + description + "] must not depend on " + List.of(forbiddenPackages)
                    + ", but:\n  " + String.join("\n  ", violations));
        }
    }

    public static Set<String> dependenciesOf(Class<?> type) {
        return dependenciesOf(type.getName());
    }

    private static Set<String> dependenciesOf(String className) {
        var model = ClassFile.of().parse(bytesOf(className));
        var dependencies = new TreeSet<String>();
        for (PoolEntry entry : model.constantPool()) {
            switch (entry) {
                case ClassEntry classEntry when !classEntry.asInternalName().startsWith("[") ->
                        dependencies.add(classEntry.asInternalName().replace('/', '.'));
                case Utf8Entry utf8 -> TYPE_IN_DESCRIPTOR.matcher(utf8.stringValue()).results()
                        .forEach(match -> dependencies.add(match.group(1).replace('/', '.')));
                default -> {
                }
            }
        }
        dependencies.remove(className);
        return dependencies;
    }

    private static Stream<String> productionClassesIn(String packageName) {
        var packagePath = packageName.replace('.', '/');
        try {
            return Collections.list(DependencyRule.class.getClassLoader().getResources(packagePath)).stream()
                    .filter(url -> url.getProtocol().equals("file"))
                    // tests live in the same packages; rules are about production code only
                    .filter(url -> !url.getPath().contains("/test-classes/"))
                    .flatMap(url -> classNamesUnder(toPath(url), packageName));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Stream<String> classNamesUnder(Path root, String packageName) {
        try (var files = Files.walk(root)) {
            return files
                    .filter(file -> file.toString().endsWith(".class"))
                    .map(file -> root.relativize(file).toString())
                    .map(relative -> packageName + "." + relative
                            .substring(0, relative.length() - ".class".length())
                            .replace(root.getFileSystem().getSeparator(), "."))
                    .toList()
                    .stream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] bytesOf(String className) {
        try (var in = DependencyRule.class.getClassLoader().getResourceAsStream(className.replace('.', '/') + ".class")) {
            if (in == null) {
                throw new IllegalArgumentException("class file not found: " + className);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path toPath(URL url) {
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
