package java8.streams;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the domain-facing report methods: map/filter/reduce/collect
 * used to answer real questions about a product catalog.
 */
class InventoryReportsTest {

    private final List<Product> catalog = List.of(
            new Product("Keyboard", "Peripherals", new BigDecimal("49.99"), 2),
            new Product("Mouse", "Peripherals", new BigDecimal("19.99"), 5),
            new Product("Monitor", "Displays", new BigDecimal("199.99"), 1)
    );

    @Test
    void should_sumPriceTimesQuantityAcrossAllProducts_when_computingTotalValue() {
        BigDecimal expected = new BigDecimal("49.99").multiply(BigDecimal.valueOf(2))
                .add(new BigDecimal("19.99").multiply(BigDecimal.valueOf(5)))
                .add(new BigDecimal("199.99").multiply(BigDecimal.valueOf(1)));

        assertEquals(0, expected.compareTo(InventoryReports.totalValue(catalog)));
    }

    @Test
    void should_returnOnlyMatchingNamesSortedAlphabetically_when_filteringByCategory() {
        assertEquals(List.of("Keyboard", "Mouse"), InventoryReports.namesInCategory(catalog, "Peripherals"));
    }

    @Test
    void should_groupProductsUnderTheirCategoryKey_when_groupingByCategory() {
        var byCategory = InventoryReports.byCategory(catalog);

        assertEquals(2, byCategory.get("Peripherals").size());
        assertEquals(1, byCategory.get("Displays").size());
    }

    @Test
    void should_returnTheLowestPricedProduct_when_findingTheCheapest() {
        assertTrue(InventoryReports.cheapest(catalog)
                .map(Product::name)
                .filter("Mouse"::equals)
                .isPresent());
    }

    @Test
    void should_joinAllNamesWithCommaSeparator_when_buildingCsv() {
        assertEquals("Keyboard, Mouse, Monitor", InventoryReports.namesAsCsv(catalog));
    }
}
