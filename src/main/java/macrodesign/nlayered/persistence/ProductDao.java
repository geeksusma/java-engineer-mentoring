package macrodesign.nlayered.persistence;

import java.util.Optional;

public class ProductDao {

    private final Database database;

    public ProductDao(Database database) {
        this.database = database;
    }

    public Optional<ProductRow> findByCode(String code) {
        database.queries++;
        return Optional.ofNullable(database.products.get(code));
    }

    public void updateStock(String code, int newStock) {
        database.queries++;
        database.products.computeIfPresent(code, (key, row) -> new ProductRow(key, row.unitPrice(), newStock));
    }
}
