package macrodesign.hexagonal.domain;

import java.util.Objects;

public record ProductCode(String value) {

    public ProductCode {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("product code must not be blank");
        }
    }
}
