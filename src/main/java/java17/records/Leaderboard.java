package java17.records;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Leaderboard {

    private Leaderboard() {
    }

    public static Optional<String> topScorer(Map<String, List<Integer>> scoresByPlayer) {
        // A local record: a named, typed tuple that only exists inside this
        // method. No more Map.Entry<String, Integer> or Object[] pairs.
        record PlayerTotal(String player, int total) {
        }

        return scoresByPlayer.entrySet().stream()
                .map(entry -> new PlayerTotal(
                        entry.getKey(),
                        entry.getValue().stream().mapToInt(Integer::intValue).sum()))
                .max(Comparator.comparingInt(PlayerTotal::total))
                .map(PlayerTotal::player);
    }
}
