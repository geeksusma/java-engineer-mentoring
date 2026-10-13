package macrodesign.hexagonal;

import org.junit.jupiter.api.Test;

import static support.DependencyRule.classesIn;

class HexagonalArchitectureTest {

    private static final String DOMAIN = "macrodesign.hexagonal.domain";
    private static final String APPLICATION = "macrodesign.hexagonal.application";
    private static final String ADAPTERS = "macrodesign.hexagonal.adapter";
    private static final String DRIVING_ADAPTERS = "macrodesign.hexagonal.adapter.in";
    private static final String DRIVEN_ADAPTERS = "macrodesign.hexagonal.adapter.out";

    @Test
    void should_dependOnNothingOutside_when_inTheDomain() {
        classesIn(DOMAIN).shouldNotDependOn(APPLICATION, ADAPTERS);
    }

    @Test
    void should_stayFrameworkAgnostic_when_inTheCore() {
        classesIn(DOMAIN, APPLICATION).shouldNotDependOn(
                "org.springframework", "jakarta", "javax.persistence", "com.fasterxml.jackson", "org.hibernate");
    }

    @Test
    void should_neverSeeAnAdapter_when_inTheApplicationCore() {
        classesIn(APPLICATION).shouldNotDependOn(ADAPTERS);
    }

    @Test
    void should_onlyMeetThroughPorts_when_adaptersAreOnOppositeSides() {
        classesIn(DRIVING_ADAPTERS).shouldNotDependOn(DRIVEN_ADAPTERS, "macrodesign.hexagonal.application.PlaceOrderService");
        classesIn(DRIVEN_ADAPTERS).shouldNotDependOn(DRIVING_ADAPTERS);
    }
}
