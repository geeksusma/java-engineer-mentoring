package java17.sealed;

/**
 * Only the types listed in {@code permits} may implement Shape. Any other
 * class trying to do so — even in the same package — fails to compile.
 */
public sealed interface Shape permits Circle, Square, Polygon {
}
