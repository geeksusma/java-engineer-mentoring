package java17.records;

import java.util.List;

public record Order(String id, List<String> items) {

    public Order {
        // A record is only shallowly immutable: without this copy, whoever
        // passed the list could keep mutating the order from the outside.
        items = List.copyOf(items);
    }
}
