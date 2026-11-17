package food.delivery;

import food.delivery.model.Customer;
import food.delivery.model.Dish;
import food.delivery.model.Order;
import food.delivery.repository.InMemoryOrderRepository;
import food.delivery.repository.OrderRepository;
import food.delivery.service.OrderService;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceTest {
    private OrderRepository repo;
    private OrderService service;
    private final PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    private final Customer bob  = new Customer(1, "Bob");
    private final Dish pizza    = new Dish("Pizza", 300);   // 300 — на границе двух профилей
    private final Dish cola     = new Dish("Cola",  100);   // 100 < любого минимума
    private final Dish salad    = new Dish("Salad", 150);

    @BeforeEach
    void setUp() {
        repo = new InMemoryOrderRepository();
        service = new OrderService(repo, registry);
    }

    @AfterEach
    void closeRegistry() {
        registry.close();
    }

    @Test
    void placeOrder_successful() {
        Order order = service.placeOrder(1, bob, Arrays.asList(pizza, cola, salad));
        assertEquals(550.0, order.getTotal());
        assertEquals(1, ((InMemoryOrderRepository) repo).size());
        assertTrue(registry.scrape().contains("application_operations_total 1.0\n"));
        assertTrue(registry.scrape().contains("application_operations_success_total 1.0\n"));
        assertTrue(registry.scrape().contains("application_operations_error_total 0.0\n"));
    }

    @Test
    void placeOrder_emptyCart_fails() {
        assertThrows(IllegalArgumentException.class,
            () -> service.placeOrder(1, bob, Arrays.asList()));
    }

    /**
     * Минимум загружается из application.properties и варьируется между профилями Maven.
     * Чтобы тест был корректен при ЛЮБОМ профиле, используем сумму (100), которая
     * меньше ОБОИХ порогов (обычная сборка: 500; промо: 200).
     */
    @Test
    void placeOrder_belowMinimum_fails() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.placeOrder(1, bob, Arrays.asList(cola)));
        assertTrue(ex.getMessage().toLowerCase().contains("minimum"),
            "message should mention minimum, got: " + ex.getMessage());
    }

    /** ERROR-сценарий: хранилище недоступно - сервис логирует ERROR и пробрасывает исключение. */
    @Test
    void placeOrder_repositoryFailure_rethrows() {
        OrderService broken = new OrderService(order -> {
            throw new IllegalStateException("Order storage is unavailable");
        }, registry);
        assertThrows(IllegalStateException.class,
            () -> broken.placeOrder(2, bob, Arrays.asList(pizza, cola, salad)));
        assertTrue(registry.scrape().contains("application_operations_total 1.0\n"));
        assertTrue(registry.scrape().contains("application_operations_success_total 0.0\n"));
        assertTrue(registry.scrape().contains("application_operations_error_total 1.0\n"));
    }
}
