package food.delivery.service;

import food.delivery.model.Customer;
import food.delivery.model.Dish;
import food.delivery.model.Order;
import food.delivery.repository.OrderRepository;

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

    /** Минимальная сумма заказа; значение берётся из application.properties. */
    public static final double MIN_ORDER_AMOUNT = loadMinOrderAmount();

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    public Order placeOrder(int id, Customer customer, List<Dish> dishes) {
        double total = dishes.stream().mapToDouble(Dish::getPrice).sum();
        if (total < MIN_ORDER_AMOUNT) {
            throw new IllegalArgumentException(
                "Order total " + total + " is below minimum " + MIN_ORDER_AMOUNT);
        }
        Order order = new Order(id, customer, dishes);
        repository.save(order);
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
