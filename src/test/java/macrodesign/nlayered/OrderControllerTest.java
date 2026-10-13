package macrodesign.nlayered;

import macrodesign.nlayered.business.OrderService;
import macrodesign.nlayered.persistence.Database;
import macrodesign.nlayered.persistence.OrderDao;
import macrodesign.nlayered.persistence.ProductDao;
import macrodesign.nlayered.persistence.ProductRow;
import macrodesign.nlayered.presentation.OrderController;
import macrodesign.nlayered.presentation.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderControllerTest {

    private Database database;
    private OrderController controller;

    @BeforeEach
    void setUp() {
        database = new Database();
        database.insertProduct(new ProductRow("KB-01", new BigDecimal("49.90"), 3));
        var productDao = new ProductDao(database);
        controller = new OrderController(new OrderService(productDao, new OrderDao(database)));
    }

    @Test
    void should_placeOrder_when_requestFlowsDownThroughEveryLayer() {
        assertEquals(new Response(201, "order #1 total 99.80"), controller.post("ana@example.com", "KB-01", 2));
        assertEquals(1, new ProductDao(database).findByCode("KB-01").orElseThrow().stock());
    }

    @Test
    void should_answerBadRequest_when_businessLayerRejectsTheOrder() {
        assertEquals(new Response(400, "only 3 left of KB-01"), controller.post("ana@example.com", "KB-01", 5));
        assertEquals(new Response(400, "unknown product: NOPE"), controller.post("ana@example.com", "NOPE", 1));
    }

    @Test
    void should_needARealDatabase_when_unitTestingTheBusinessLayer() {
        // OrderService takes concrete DAOs: there is no seam to plug a fake in,
        // so even a "unit" test of a business rule has to build the database.
        var service = new OrderService(new ProductDao(database), new OrderDao(database));

        service.placeOrder("ana@example.com", "KB-01", 1);

        assertEquals(3, database.queries());
    }
}
