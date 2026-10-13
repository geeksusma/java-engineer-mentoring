package macrodesign.hexagonal;

import macrodesign.hexagonal.adapter.in.web.Response;
import macrodesign.hexagonal.domain.Product;
import macrodesign.hexagonal.domain.ProductCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DrivingAdaptersTest {

    private HexagonalApp app;

    @BeforeEach
    void setUp() {
        app = new HexagonalApp();
        app.catalog.save(new Product(new ProductCode("KB-01"), new BigDecimal("49.90"), 3));
    }

    @Test
    void should_driveTheSameUseCase_when_comingFromWebOrCommandLine() {
        assertEquals(new Response(201, "order #1 total 49.90"), app.web.post("ana@example.com", "KB-01", 1));
        assertEquals("OK 2", app.cli.run("order bob@example.com KB-01 1"));

        assertEquals(2, app.orders.all().size());
        assertEquals(List.of(
                "to ana@example.com: your order #1 is confirmed",
                "to bob@example.com: your order #2 is confirmed"), app.confirmations.outbox());
    }

    @Test
    void should_translateDomainErrors_when_eachAdapterSpeaksItsOwnProtocol() {
        assertEquals(new Response(400, "only 3 left of KB-01"), app.web.post("ana@example.com", "KB-01", 9));
        assertEquals("REJECTED only 3 left of KB-01", app.cli.run("order ana@example.com KB-01 9"));
    }
}
