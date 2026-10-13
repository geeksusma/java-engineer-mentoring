package macrodesign.nlayered;

import macrodesign.nlayered.business.OrderService;
import macrodesign.nlayered.presentation.OrderController;
import org.junit.jupiter.api.Test;
import support.DependencyRule;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static support.DependencyRule.classesIn;

class LayeringRulesTest {

    private static final String PRESENTATION = "macrodesign.nlayered.presentation";
    private static final String BUSINESS = "macrodesign.nlayered.business";
    private static final String PERSISTENCE = "macrodesign.nlayered.persistence";

    @Test
    void should_onlyDependDownwards_when_layeringIsRespected() {
        classesIn(BUSINESS).shouldNotDependOn(PRESENTATION);
        classesIn(PERSISTENCE).shouldNotDependOn(BUSINESS, PRESENTATION);
    }

    @Test
    void should_leakPersistenceTypesToPresentation_when_rowsAreTheModel() {
        // the controller never imports the persistence package, yet it touches
        // OrderRow: the row returned by the service travels up through it
        assertTrue(DependencyRule.dependenciesOf(OrderController.class)
                .contains("macrodesign.nlayered.persistence.OrderRow"));
        assertThrows(AssertionError.class, () -> classesIn(PRESENTATION).shouldNotDependOn(PERSISTENCE));
    }

    @Test
    void should_dependOnPersistence_when_businessLayerIsClassic() {
        assertTrue(DependencyRule.dependenciesOf(OrderService.class)
                .contains("macrodesign.nlayered.persistence.ProductRow"));

        // the rule hexagonal and clean architecture enforce fails here
        assertThrows(AssertionError.class, () -> classesIn(BUSINESS).shouldNotDependOn(PERSISTENCE));
    }
}
