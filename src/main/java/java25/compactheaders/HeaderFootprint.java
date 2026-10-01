package java25.compactheaders;

import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.ThreadMXBean;

import java.lang.management.ManagementFactory;
import java.util.function.Supplier;

/**
 * Meant to run in its own JVM (see HeaderFootprintTest) with and without
 * -XX:+UseCompactObjectHeaders, so both layouts can be compared.
 */
public final class HeaderFootprint {

    private static final int INSTANCES = 1_000_000;

    record Point(int x, int y) {
    }

    private HeaderFootprint() {
    }

    public static void main(String[] args) {
        System.out.println("compactHeaders=" + compactHeadersEnabled());
        System.out.println("object=" + bytesPerInstance(Object::new));
        System.out.println("point=" + bytesPerInstance(() -> new Point(1, 2)));
    }

    public static boolean compactHeadersEnabled() {
        var hotSpot = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        return Boolean.parseBoolean(hotSpot.getVMOption("UseCompactObjectHeaders").getValue());
    }

    static long bytesPerInstance(Supplier<Object> factory) {
        var threads = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        // The holder array is allocated before the first reading, so only
        // the instances themselves are counted. Storing them in the array
        // also stops the JIT from optimizing the allocations away.
        var holder = new Object[INSTANCES];
        long before = threads.getCurrentThreadAllocatedBytes();
        for (int i = 0; i < INSTANCES; i++) {
            holder[i] = factory.get();
        }
        long after = threads.getCurrentThreadAllocatedBytes();
        return Math.round((double) (after - before) / holder.length);
    }
}
