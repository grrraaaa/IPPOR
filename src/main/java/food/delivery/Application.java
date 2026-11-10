package food.delivery;

import food.delivery.model.Customer;
import food.delivery.model.Dish;
import food.delivery.model.Order;
import food.delivery.repository.InMemoryOrderRepository;
import food.delivery.repository.OrderRepository;
import food.delivery.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        OrderRepository repo = new InMemoryOrderRepository();
        OrderService service = new OrderService(repo);

        Customer alice = new Customer(1, "Alice");
        Dish pizza = new Dish("Pizza", 300);
        Dish sushi = new Dish("Sushi", 450);

        LOG.info("Food Delivery demo, minimum order amount: {}", OrderService.MIN_ORDER_AMOUNT);

        // Сценарий 1: успешное оформление (INFO)
        placeOrder(service, 1, alice, Arrays.asList(pizza, sushi));
        // Сценарий 2: сумма ниже минимума (WARN)
        placeOrder(service, 2, alice, Arrays.asList(pizza));
        // Сценарий 3: сбой хранилища (ERROR)
        OrderService broken = new OrderService(order -> {
            throw new IllegalStateException("Order storage is unavailable");
        });
        placeOrder(broken, 3, alice, Arrays.asList(pizza, sushi));
    }

    private static void placeOrder(OrderService service, int id, Customer customer, List<Dish> dishes) {
        try {
            Order order = service.placeOrder(id, customer, dishes);
            LOG.info("ACCEPTED order #{} for {}", order.getId(), order.getCustomer().getName());
        } catch (RuntimeException e) {
            LOG.info("REJECTED order #{}: {}", id, e.getMessage());
        }
    }
}
