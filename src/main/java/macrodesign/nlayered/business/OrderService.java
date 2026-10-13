package macrodesign.nlayered.business;

import macrodesign.nlayered.persistence.OrderDao;
import macrodesign.nlayered.persistence.OrderRow;
import macrodesign.nlayered.persistence.ProductDao;

import java.math.BigDecimal;

/**
 * The business layer of a classic N-layered app. It depends on the concrete
 * DAOs and works with their row types: the arrows point down to the database.
 */
public class OrderService {

    private final ProductDao productDao;
    private final OrderDao orderDao;

    public OrderService(ProductDao productDao, OrderDao orderDao) {
        this.productDao = productDao;
        this.orderDao = orderDao;
    }

    public OrderRow placeOrder(String customerEmail, String productCode, int quantity) {
        if (quantity <= 0) {
            throw new OrderRejectedException("quantity must be positive: " + quantity);
        }
        var product = productDao.findByCode(productCode)
                .orElseThrow(() -> new OrderRejectedException("unknown product: " + productCode));
        if (product.stock() < quantity) {
            throw new OrderRejectedException("only " + product.stock() + " left of " + productCode);
        }
        productDao.updateStock(productCode, product.stock() - quantity);
        var total = product.unitPrice().multiply(BigDecimal.valueOf(quantity));
        return orderDao.insert(customerEmail, productCode, quantity, total);
    }
}
