package macrodesign.clean.entities;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Enterprise business rule: what a valid nickname is holds for every
 * application of the platform (web, launcher, mobile), so it lives in the
 * innermost circle.
 */
public record Nickname(String value) {

    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_]{3,16}");

    public Nickname {
        if (value == null || !VALID.matcher(value).matches()) {
            throw new InvalidNicknameException("nickname must be 3-16 letters, digits or underscores");
        }
    }

    /**
     * "Neo" and "neo" are the same nickname for uniqueness purposes.
     */
    public String canonical() {
        return value.toLowerCase(Locale.ROOT);
    }
}
