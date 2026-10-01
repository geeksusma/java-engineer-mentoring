package java17.sealed;

/**
 * Records are implicitly final, so they satisfy the "final, sealed or
 * non-sealed" rule for permitted subtypes without any extra keyword.
 */
public record Circle(double radius) implements Shape {
}
