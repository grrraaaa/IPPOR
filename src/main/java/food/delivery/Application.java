package food.delivery;

import food.delivery.model.Customer;
import food.delivery.model.Dish;
import food.delivery.model.Order;
import food.delivery.repository.InMemoryOrderRepository;
import food.delivery.repository.OrderRepository;
import food.delivery.service.OrderService;

import java.util.Arrays;

public class Application {

    public static void main(String[] args) {
        OrderRepository repo = new InMemoryOrderRepository();
        OrderService service = new OrderService(repo);

        Customer alice = new Customer(1, "Alice");
        Dish pizza = new Dish("Pizza", 300);    
        Dish cola  = new Dish("Cola",  100);

        System.out.println("--- Food Delivery demo ---");
        System.out.printf ("Configured minimum order amount: %.2f%n",
                           OrderService.MIN_ORDER_AMOUNT);

        try {
            Order order = service.placeOrder(1, alice, Arrays.asList(pizza));
            System.out.printf ("ACCEPTED  Order #%d for %s, total=%.2f%n",
                               order.getId(), order.getCustomer().getName(),
                               order.getTotal());
        } catch (IllegalArgumentException e) {
            System.out.println("REJECTED  " + e.getMessage());
        }
    }

    private static int repoSize(OrderRepository repo) {
        return (repo instanceof InMemoryOrderRepository)
                ? ((InMemoryOrderRepository) repo).size() : -1;
    }
}
