package java17.gc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import support.ChildJvm;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GcInspectorTest {

    @Test
    void should_reportAtLeastOneCollector_when_inspectingTheCurrentJvm() {
        assertFalse(GcInspector.activeCollectorNames().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
            "-XX:+UseSerialGC,   Copy",
            "-XX:+UseParallelGC, PS Scavenge",
            "-XX:+UseG1GC,       G1 Young Generation",
            "-XX:+UseZGC,        ZGC Minor Cycles",
    })
    void should_runTheRequestedCollector_when_selectedWithAJvmFlag(String flag, String expectedCollector)
            throws Exception {
        var result = ChildJvm.run(GcInspector.class, flag);

        assertEquals(0, result.exitCode(), result.output());
        assertTrue(Set.copyOf(result.output().lines().toList()).contains(expectedCollector), result.output());
    }

    @Test
    void should_refuseToStart_when_askingForTheRemovedCmsCollector() throws Exception {
        // CMS was deprecated in Java 9 and removed in Java 14 (JEP 363).
        var result = ChildJvm.run(GcInspector.class, "-XX:+UseConcMarkSweepGC");

        assertNotEquals(0, result.exitCode());
        assertTrue(result.output().contains("Unrecognized VM option 'UseConcMarkSweepGC'"), result.output());
    }
}
