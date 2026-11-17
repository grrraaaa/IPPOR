package food.delivery;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import food.delivery.model.Customer;
import food.delivery.model.Dish;
import food.delivery.model.Order;
import food.delivery.repository.InMemoryOrderRepository;
import food.delivery.repository.OrderRepository;
import food.delivery.service.OrderService;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws IOException {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        if (args.length == 1 && "--demo".equals(args[0])) {
            try {
                runDemo(registry);
            } finally {
                registry.close();
            }
            return;
        }

        OrderService service = new OrderService(new InMemoryOrderRepository(), registry);
        AtomicInteger orderIds = new AtomicInteger();
        Customer alice = new Customer(1, "Alice");
        Dish pizza = new Dish("Pizza", 300);
        Dish sushi = new Dish("Sushi", 450);
        String host = System.getProperty("delivery.host", "127.0.0.1");
        HttpServer server = HttpServer.create(new InetSocketAddress(host, 8081), 0);
        server.createContext("/", exchange -> {
            switch (exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath()) {
                case "GET /metrics":
                    exchange.getResponseHeaders().set("Content-Type", "text/plain; version=0.0.4; charset=utf-8");
                    respond(exchange, 200, registry.scrape());
                    break;
                case "POST /orders":
                    submitOrder(exchange, service, orderIds.incrementAndGet(), alice,
                        Arrays.asList(pizza, sushi, sushi));
                    break;
                case "POST /orders/below-minimum":
                    submitOrder(exchange, service, orderIds.incrementAndGet(), alice,
                        Collections.singletonList(pizza));
                    break;
                default:
                    respond(exchange, 404, "Endpoint not found\n");
            }
        });
        server.start();
        LOG.info("Food Delivery listening on {}:8081, minimum order amount: {}", host, OrderService.MIN_ORDER_AMOUNT);
    }

    private static void submitOrder(HttpExchange exchange, OrderService service, int id,
                                    Customer customer, List<Dish> dishes) throws IOException {
        Order order;
        try {
            order = service.placeOrder(id, customer, dishes);
        } catch (IllegalArgumentException e) {
            respond(exchange, 400, e.getMessage() + "\n");
            return;
        } catch (RuntimeException e) {
            respond(exchange, 500, "Order could not be saved\n");
            return;
        }
        respond(exchange, 201, "Created order #" + order.getId() + "\n");
    }

    private static void respond(HttpExchange exchange, int status, String message) throws IOException {
        byte[] body = message.getBytes(StandardCharsets.UTF_8);
        try {
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
        } finally {
            exchange.close();
        }
    }

    private static void runDemo(PrometheusMeterRegistry registry) {
        OrderRepository repo = new InMemoryOrderRepository();
        OrderService service = new OrderService(repo, registry);

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
        }, registry);
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
