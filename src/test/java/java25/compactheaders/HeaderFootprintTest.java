package java25.compactheaders;

import org.junit.jupiter.api.Test;
import support.ChildJvm;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeaderFootprintTest {

    @Test
    void should_use16BytesPerObject_when_compactHeadersAreDisabled() throws Exception {
        var result = ChildJvm.run(HeaderFootprint.class, "-XX:-UseCompactObjectHeaders");

        assertEquals("false", result.valueOf("compactHeaders"));
        // 12-byte header, padded to the 8-byte alignment.
        assertEquals("16", result.valueOf("object"));
        // 12-byte header + 2 ints (8 bytes) = 20, padded to 24.
        assertEquals("24", result.valueOf("point"));
    }

    @Test
    void should_halveAnEmptyObject_when_compactHeadersAreEnabled() throws Exception {
        var result = ChildJvm.run(HeaderFootprint.class, "-XX:+UseCompactObjectHeaders");

        assertEquals("true", result.valueOf("compactHeaders"));
        // 8-byte header, nothing to pad.
        assertEquals("8", result.valueOf("object"));
        // 8-byte header + 2 ints (8 bytes) = 16, no padding needed.
        assertEquals("16", result.valueOf("point"));
    }
}
