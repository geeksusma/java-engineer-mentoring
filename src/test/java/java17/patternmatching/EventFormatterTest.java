package java17.patternmatching;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import java17.patternmatching.Event.Login;
import java17.patternmatching.Event.Logout;
import java17.patternmatching.Event.Purchase;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventFormatterTest {

    @Test
    void should_deconstructRecordComponents_when_matchingWithRecordPatterns() {
        assertEquals("ana logged in", EventFormatter.format(new Login("ana")));
        assertEquals("ana logged out", EventFormatter.format(new Logout("ana")));
    }

    @Test
    void should_pickGuardedCase_when_purchaseIsLarge() {
        assertEquals("LARGE purchase by bob: 2500",
                EventFormatter.format(new Purchase("bob", new BigDecimal("2500"))));
    }

    @Test
    void should_fallThroughToUnguardedCase_when_guardDoesNotMatch() {
        assertEquals("bob bought for 20",
                EventFormatter.format(new Purchase("bob", new BigDecimal("20"))));
    }
}
