package food.delivery.model;
import java.util.Objects;
public class Customer {
    private final int id;
    private final String name;
    public Customer(int id, String name) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
    }
    public int getId() { return id; }
    public String getName() { return name; }
}
