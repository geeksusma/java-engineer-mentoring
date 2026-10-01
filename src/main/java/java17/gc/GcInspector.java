package java17.gc;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

public final class GcInspector {

    private GcInspector() {
    }

    /**
     * Each collector registers one or more MXBeans named after itself, so
     * their names reveal which GC the running JVM picked.
     */
    public static List<String> activeCollectorNames() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .map(GarbageCollectorMXBean::getName)
                .toList();
    }

    public static void main(String[] args) {
        activeCollectorNames().forEach(System.out::println);
    }
}
