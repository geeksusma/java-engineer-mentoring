package macrodesign.modular;

import org.junit.jupiter.api.Test;

import static support.DependencyRule.classesIn;

class ModuleBoundariesTest {

    private static final String CATALOG = "macrodesign.modular.catalog";
    private static final String ORDERING = "macrodesign.modular.ordering";

    @Test
    void should_onlyUseThePublicApi_when_orderingTalksToCatalog() {
        classesIn(ORDERING).shouldNotDependOn(CATALOG + ".internal", CATALOG + ".server", CATALOG + ".client",
                CATALOG + ".CatalogModule");
    }

    @Test
    void should_notKnowItsConsumers_when_beingTheCatalog() {
        classesIn(CATALOG).shouldNotDependOn(ORDERING);
    }

    @Test
    void should_dependOnNoImplementation_when_beingThePublicApi() {
        classesIn(CATALOG + ".api").shouldNotDependOn(CATALOG + ".internal", CATALOG + ".server", CATALOG + ".client");
    }
}
