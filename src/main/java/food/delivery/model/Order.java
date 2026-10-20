package food.delivery.model;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
public class Order {
    private final int id;
    private final Customer customer;
    private final List<Dish> dishes;
    private final double total;
    public Order(int id, Customer customer, List<Dish> dishes) {
        this.id = id;
        this.customer = Objects.requireNonNull(customer, "customer");
        this.dishes = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(dishes, "dishes")));
        this.total = this.dishes.stream().mapToDouble(Dish::getPrice).sum();
    }
    public int getId() { return id; }
    public Customer getCustomer() { return customer; }
    public List<Dish> getDishes() { return dishes; }
    public double getTotal() { return total; }
}
