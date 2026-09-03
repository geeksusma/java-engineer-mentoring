package java8.defaultmethods;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * FlyingFish implements two interfaces that both provide a default move().
 * This only compiles because FlyingFish explicitly overrides move() and
 * resolves the ambiguity itself — the diamond problem is caught at compile
 * time, not left as a runtime surprise.
 */
class DiamondProblemTest {

    @Test
    void should_combineBothParentDefaults_when_theOverrideDelegatesToBothViaSuper() {
        assertEquals("flying and swimming", new FlyingFish().move());
    }
}
