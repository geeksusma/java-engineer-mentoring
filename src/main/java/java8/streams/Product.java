package java8.streams;

import java.math.BigDecimal;

public record Product(String name, String category, BigDecimal price, int quantity) {
}
