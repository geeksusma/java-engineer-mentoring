package java25.primitivepatterns;

/**
 * Primitive types in patterns, instanceof and switch are still a preview
 * feature in Java 25 (JEP 507) — compile and run with --enable-preview.
 */
public final class PrimitivePatterns {

    public record SensorReading(double value) {
    }

    private PrimitivePatterns() {
    }

    /**
     * {@code value instanceof byte b} means "can this int be converted to a
     * byte without losing information?" — and if so, binds the result.
     */
    public static String narrowestType(int value) {
        if (value instanceof byte b) {
            return "byte " + b;
        }
        if (value instanceof short s) {
            return "short " + s;
        }
        return "int " + value;
    }

    public static boolean isExactInt(double value) {
        return value instanceof int;
    }

    public static boolean fitsExactlyInFloat(int value) {
        return value instanceof float;
    }

    public static String classifyHttpStatus(int status) {
        return switch (status) {
            case 200 -> "OK";
            case 404 -> "Not Found";
            case int s when s >= 200 && s < 300 -> "Success (" + s + ")";
            case int s when s >= 400 && s < 500 -> "Client error (" + s + ")";
            case int s when s >= 500 && s < 600 -> "Server error (" + s + ")";
            case int s -> "Unknown (" + s + ")";
        };
    }

    public static String describeFileSize(long bytes) {
        return switch (bytes) {
            case 0L -> "empty";
            case long b when b < 1024 -> b + " B";
            case long b when b < 1024 * 1024 -> (b / 1024) + " KB";
            case long b -> (b / (1024 * 1024)) + " MB";
        };
    }

    public static String toggleLabel(boolean enabled) {
        // Exhaustive: true and false are the only two values.
        return switch (enabled) {
            case true -> "ON";
            case false -> "OFF";
        };
    }

    public static String describe(SensorReading reading) {
        // The nested primitive pattern int whole only matches when the
        // double component converts to int exactly.
        return switch (reading) {
            case SensorReading(int whole) -> "exact reading " + whole;
            case SensorReading(double approx) -> "approximate reading " + approx;
        };
    }
}
