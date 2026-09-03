package java8.streams;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * filter() is typed as Predicate<T> — a lambda is just the shorthand syntax
 * for implementing that single abstract method. These tests prove the three
 * ways of satisfying it are interchangeable, and that a no-op ("empty")
 * lambda is a legitimate way to make one stage of a pipeline do nothing.
 */
class FunctionalInterfaceTest {

    private final List<Product> catalog = List.of(
            new Product("Keyboard", "Peripherals", new BigDecimal("49.99"), 2),
            new Product("Mouse", "Peripherals", new BigDecimal("19.99"), 5),
            new Product("Monitor", "Displays", new BigDecimal("199.99"), 1)
    );

    @Test
    void should_produceTheSameResult_when_theSamePredicateIsSuppliedAsALambda() {
        assertEquals(List.of("Keyboard", "Mouse"),
                InventoryReports.namesMatching(catalog, p -> p.category().equals("Peripherals")));
    }

    @Test
    void should_produceTheSameResult_when_theSamePredicateIsSuppliedAsAMethodReference() {
        assertEquals(List.of("Monitor"),
                InventoryReports.namesMatching(catalog, this::isExpensive));
    }

    @Test
    void should_produceTheSameResult_when_theSamePredicateIsSuppliedAsAStoredVariable() {
        Predicate<Product> isPeripheral = p -> p.category().equals("Peripherals");

        assertEquals(List.of("Keyboard", "Mouse"), InventoryReports.namesMatching(catalog, isPeripheral));
    }

    @Test
    void should_keepEveryElement_when_theNoOpPredicateIsUsedBecauseThereIsNoCategoryToFilterBy() {
        assertEquals(List.of("Keyboard", "Monitor", "Mouse"),
                InventoryReports.namesInCategoryOrAll(catalog, null));
    }

    @Test
    void should_stillFilterNormally_when_aCategoryIsSuppliedInsteadOfTheNoOpPredicate() {
        assertEquals(List.of("Monitor"), InventoryReports.namesInCategoryOrAll(catalog, "Displays"));
    }

    private boolean isExpensive(Product product) {
        return product.price().compareTo(new BigDecimal("100")) > 0;
    }
}
