package macrodesign.nlayered.persistence;

import java.util.Optional;

public class OrderDao {

    private final Database database;

    public OrderDao(Database database) {
        this.database = database;
    }

    /**
     * Mimics an auto-increment primary key: the id comes from the database.
     */
    public OrderRow insert(String customerEmail, String productCode, int quantity, java.math.BigDecimal total) {
        database.queries++;
        var row = new OrderRow(database.nextOrderId++, customerEmail, productCode, quantity, total);
        database.orders.put(row.id(), row);
        return row;
    }

    public Optional<OrderRow> findById(long id) {
        database.queries++;
        return Optional.ofNullable(database.orders.get(id));
    }
}
