package java8.streams;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Small collection of stream-based queries over a product catalog, used to
 * demonstrate map/filter/reduce/collect without hiding them behind
 * unrelated business complexity.
 */
public final class InventoryReports {

    private InventoryReports() {
    }

    public static BigDecimal totalValue(List<Product> products) {
        return products.stream()
                .map(p -> p.price().multiply(BigDecimal.valueOf(p.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static List<String> namesInCategory(List<Product> products, String category) {
        return products.stream()
                .filter(p -> p.category().equals(category))
                .map(Product::name)
                .sorted()
                .toList();
    }

    public static List<String> namesMatching(List<Product> products, Predicate<Product> filter) {
        return products.stream()
                .filter(filter)
                .map(Product::name)
                .sorted()
                .toList();
    }

    public static List<String> namesInCategoryOrAll(List<Product> products, String category) {
        Predicate<Product> categoryFilter = category == null
                ? p -> true // no-op predicate: keep every element when there's nothing to filter by
                : p -> p.category().equals(category);

        return namesMatching(products, categoryFilter);
    }

    public static Map<String, List<Product>> byCategory(List<Product> products) {
        return products.stream()
                .collect(Collectors.groupingBy(Product::category));
    }

    public static Optional<Product> cheapest(List<Product> products) {
        return products.stream()
                .min(Comparator.comparing(Product::price));
    }

    public static String namesAsCsv(List<Product> products) {
        return products.stream()
                .map(Product::name)
                .collect(Collectors.joining(", "));
    }
}
