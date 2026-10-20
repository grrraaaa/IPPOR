package food.delivery.model;
import java.util.Objects;
public class Dish {
    private final String name;
    private final double price;
    public Dish(String name, double price) {
        if (price < 0) throw new IllegalArgumentException("price < 0");
        this.name = Objects.requireNonNull(name, "name");
        this.price = price;
    }
    public String getName() { return name; }
    public double getPrice() { return price; }
}
