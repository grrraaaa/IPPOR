package food.delivery.service;

import food.delivery.model.Customer;
import food.delivery.model.Dish;
import food.delivery.model.Order;
import food.delivery.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/**
 * Сервис оформления заказов.
 * Минимальная сумма заказа подставляется Maven-профилем через src/main/resources/application.properties
 * (фильтрация ресурсов включена в pom.xml).
 *
 * - обычная сборка:                mvn package       -> min = 500
 * - промо-сборка:                  mvn -P promo package -> min = 1000
 */
public class OrderService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderService.class);

    /** Минимальная сумма заказа; значение берётся из application.properties. */
    public static final double MIN_ORDER_AMOUNT = loadMinOrderAmount();{

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    public Order placeOrder(int id, Customer customer, List<Dish> dishes) {
        LOG.info("Delivery order #{} received: {} dish(es)", id, dishes.size());
        double total = dishes.stream().mapToDouble(Dish::getPrice).sum();
        if (total < MIN_ORDER_AMOUNT) {
            // некорректная операция клиента, не сбой системы -> WARN
            LOG.warn("Delivery order #{} rejected: total {} is below minimum {}", id, total, MIN_ORDER_AMOUNT);
            throw new IllegalArgumentException(
                "Order total " + total + " is below minimum " + MIN_ORDER_AMOUNT);
        }
        Order order = new Order(id, customer, dishes);
        try {
            repository.save(order);
        } catch (RuntimeException e) {
            // заказ клиента корректен, но система не смогла его сохранить -> ERROR
            LOG.error("Delivery order #{} failed: could not save order", id, e);
            throw e;
        }
        LOG.info("Delivery order #{} placed for '{}', total={}", id, customer.getName(), total);
        return order;
    }

    private static double loadMinOrderAmount() {
        try (InputStream in = OrderService.class.getResourceAsStream(
                "/application.properties")) {
            if (in == null) {
                return 500.0;
            }
            Properties p = new Properties();
            p.load(in);
            String v = p.getProperty("min.order.amount");
            return v == null ? 500.0 : Double.parseDouble(v);
        } catch (IOException | NumberFormatException e) {
            return 500.0;
        }
    }
}
