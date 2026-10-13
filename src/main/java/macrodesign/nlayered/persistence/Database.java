package macrodesign.nlayered.persistence;

import java.util.HashMap;
import java.util.Map;

/**
 * Stands in for a real relational database: two tables kept in memory, plus a
 * query counter so tests can see how often the business layer hits it.
 */
public final class Database {

    final Map<String, ProductRow> products = new HashMap<>();
    final Map<Long, OrderRow> orders = new HashMap<>();
    long nextOrderId = 1;
    int queries;

    public void insertProduct(ProductRow row) {
        products.put(row.code(), row);
    }

    public int queries() {
        return queries;
    }
}
