package java25.primitivepatterns;

/**
 * Before JEP 507, instanceof and switch patterns only worked on reference
 * types, and switch couldn't take a long, float, double or boolean at all.
 */
public final class LegacyNarrowing {

    private LegacyNarrowing() {
    }

    public static String narrowestType(int value) {
        if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            return "byte " + (byte) value;
        }
        if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            return "short " + (short) value;
        }
        return "int " + value;
    }

    public static String describeFileSize(long bytes) {
        // switch (bytes) { ... } would not compile before Java 25 + preview.
        if (bytes == 0L) {
            return "empty";
        } else if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return (bytes / 1024) + " KB";
        }
        return (bytes / (1024 * 1024)) + " MB";
    }
}
