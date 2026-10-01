package java17.patternmatching;

/**
 * The pre-Java-16 way: test the type, then cast it again by hand.
 */
public final class LegacyDescriber {

    private LegacyDescriber() {
    }

    public static String describe(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Integer) {
            Integer number = (Integer) value;
            if (number < 0) {
                return "negative integer " + number;
            }
            return "integer " + number;
        }
        if (value instanceof String) {
            String text = (String) value;
            if (text.isBlank()) {
                return "blank string";
            }
            return "string of length " + text.length();
        }
        if (value instanceof int[]) {
            int[] numbers = (int[]) value;
            return "int array of " + numbers.length;
        }
        return "unknown: " + value.getClass().getSimpleName();
    }
}
