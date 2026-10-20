package food.delivery;

import food.delivery.model.Customer;
import food.delivery.model.Dish;
import food.delivery.model.Order;
import food.delivery.repository.InMemoryOrderRepository;
import food.delivery.repository.OrderRepository;
import food.delivery.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceTest {
    private OrderRepository repo;
    private OrderService service;
    private final Customer bob  = new Customer(1, "Bob");
    private final Dish pizza    = new Dish("Pizza", 300);   // 300 — на границе двух профилей
    private final Dish cola     = new Dish("Cola",  100);   // 100 < любого минимума
    private final Dish salad    = new Dish("Salad", 150);

    @BeforeEach
    void setUp() {
        repo = new InMemoryOrderRepository();
        service = new OrderService(repo);
    }

    @Test
    void placeOrder_successful() {
        Order order = service.placeOrder(1, bob, Arrays.asList(pizza, cola, salad));
        assertEquals(550.0, order.getTotal());
        assertEquals(1, ((InMemoryOrderRepository) repo).size());
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
}
