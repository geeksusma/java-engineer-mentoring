package java25.virtualthreads;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestContextTest {

    @Test
    void should_readBoundValueDeepInTheCallStack_when_insideTheScope() {
        assertEquals("processed request req-42", RequestContext.handle("req-42"));
    }

    @Test
    void should_beUnbound_when_outsideAnyScope() {
        assertFalse(RequestContext.REQUEST_ID.isBound());
        assertThrows(NoSuchElementException.class, RequestContext.REQUEST_ID::get);
        assertEquals("none", RequestContext.currentRequestIdOr("none"));
    }

    @Test
    void should_restoreOuterValue_when_nestedRebindingEnds() {
        ScopedValue.where(RequestContext.REQUEST_ID, "outer").run(() -> {
            ScopedValue.where(RequestContext.REQUEST_ID, "inner").run(() ->
                    assertEquals("inner", RequestContext.REQUEST_ID.get()));

            assertEquals("outer", RequestContext.REQUEST_ID.get());
        });
    }
}
