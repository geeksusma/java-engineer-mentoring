package java17.records;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderTest {

    @Test
    void should_notChange_when_callerMutatesTheListItPassedIn() {
        var items = new ArrayList<>(List.of("book"));
        var order = new Order("o-1", items);

        items.add("pen");

        assertEquals(List.of("book"), order.items());
    }

    @Test
    void should_rejectMutation_when_modifyingTheReturnedList() {
        var order = new Order("o-1", List.of("book"));

        assertThrows(UnsupportedOperationException.class, () -> order.items().add("pen"));
    }

    @Test
    void should_findTopScorer_when_groupingWithALocalRecord() {
        var scores = Map.of(
                "ana", List.of(10, 20),
                "bob", List.of(50),
                "cid", List.of(5, 5, 5));

        assertEquals(Optional.of("bob"), Leaderboard.topScorer(scores));
    }
}
