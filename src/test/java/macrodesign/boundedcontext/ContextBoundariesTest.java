package macrodesign.boundedcontext;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static support.DependencyRule.classesIn;

class ContextBoundariesTest {

    private static final String SALES = "macrodesign.boundedcontext.sales";
    private static final String SHIPPING = "macrodesign.boundedcontext.shipping";
    private static final String CONTRACTS = "macrodesign.boundedcontext.contracts";

    @Test
    void should_shareOnlyThePublishedLanguage_when_contextsIntegrate() {
        classesIn(SALES).shouldNotDependOn(SHIPPING);
        classesIn(SHIPPING).shouldNotDependOn(SALES);
    }

    @Test
    void should_dependOnNeitherContext_when_beingTheContract() {
        classesIn(CONTRACTS).shouldNotDependOn(SALES, SHIPPING);
    }

    @Test
    void should_modelTheSameWordDifferently_when_contextsDiffer() {
        assertEquals(List.of("sku", "name", "price"), componentsOf(macrodesign.boundedcontext.sales.Product.class));
        assertEquals(List.of("sku", "weightGrams", "fragile"), componentsOf(macrodesign.boundedcontext.shipping.Product.class));
    }

    private static List<String> componentsOf(Class<? extends Record> type) {
        return Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList();
    }
}
