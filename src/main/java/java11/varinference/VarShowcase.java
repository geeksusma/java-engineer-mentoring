package java11.varinference;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class VarShowcase {

    private VarShowcase() {
    }

    public static int sumLengthsWithEnhancedFor(List<String> words) {
        var total = 0;
        for (var word : words) {
            total += word.length();
        }
        return total;
    }

    public static Map<String, Integer> lengthsByWord(List<String> words) {
        var lengths = new LinkedHashMap<String, Integer>();
        for (var word : words) {
            lengths.put(word, word.length());
        }
        return lengths;
    }

    public static List<String> upperCaseAll(List<String> words) {
        Function<String, String> toUpper = (var word) -> word.toUpperCase();
        return words.stream().map(toUpper).toList();
    }

    public static String describeUsingAnonymousClassExtraMember(String value) {
        var describer = new Object() {
            String reversed() {
                return new StringBuilder(value).reverse().toString();
            }
        };
        // Only possible because `var` kept the anonymous class's real type.
        // Declaring `Object describer = ...` would hide reversed() entirely.
        return describer.reversed();
    }
}
