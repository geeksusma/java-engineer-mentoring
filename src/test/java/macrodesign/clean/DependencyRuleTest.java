package macrodesign.clean;

import org.junit.jupiter.api.Test;

import static support.DependencyRule.classesIn;

/**
 * "Source code dependencies can only point inwards." — the Dependency Rule.
 */
class DependencyRuleTest {

    private static final String ENTITIES = "macrodesign.clean.entities";
    private static final String USE_CASES = "macrodesign.clean.usecases";
    private static final String INTERFACE_ADAPTERS = "macrodesign.clean.interfaceadapters";
    private static final String FRAMEWORKS = "macrodesign.clean.frameworks";

    @Test
    void should_knowNothingOutside_when_inTheEntitiesCircle() {
        classesIn(ENTITIES).shouldNotDependOn(USE_CASES, INTERFACE_ADAPTERS, FRAMEWORKS);
    }

    @Test
    void should_onlyKnowEntities_when_inTheUseCasesCircle() {
        classesIn(USE_CASES).shouldNotDependOn(INTERFACE_ADAPTERS, FRAMEWORKS);
    }

    @Test
    void should_onlyReachDriversThroughGateways_when_inTheInterfaceAdaptersCircle() {
        // controllers and presenters never touch the database driver; only
        // gateways adapt to the outer circle
        classesIn(INTERFACE_ADAPTERS + ".controllers", INTERFACE_ADAPTERS + ".presenters")
                .shouldNotDependOn(FRAMEWORKS);
    }

    @Test
    void should_knowNothingAboutPlayers_when_beingTheDatabaseDriver() {
        // CleanApp (the main component) wires everything; the driver itself is
        // a reusable tool that knows rows, not entities
        classesIn(FRAMEWORKS + ".database").shouldNotDependOn(ENTITIES, USE_CASES, INTERFACE_ADAPTERS);
    }
}
