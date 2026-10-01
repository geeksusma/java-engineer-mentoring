package java17.patternmatching;

public final class ModernDescriber {

    private ModernDescriber() {
    }

    /**
     * Same behaviour as {@link LegacyDescriber#describe(Object)}: type test,
     * cast and binding happen in one step, guards replace nested ifs and
     * null is just another case.
     */
    public static String describe(Object value) {
        return switch (value) {
            case null -> "null";
            case Integer number when number < 0 -> "negative integer " + number;
            case Integer number -> "integer " + number;
            case String text when text.isBlank() -> "blank string";
            case String text -> "string of length " + text.length();
            case int[] numbers -> "int array of " + numbers.length;
            default -> "unknown: " + value.getClass().getSimpleName();
        };
    }

    /**
     * Flow scoping: the binding {@code text} is in scope after the if,
     * because the compiler knows we only get there when the match succeeded.
     */
    public static int lengthOrMinusOne(Object value) {
        if (!(value instanceof String text)) {
            return -1;
        }
        return text.length();
    }
}
