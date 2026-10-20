package food.delivery.repository;
import food.delivery.model.Order;
import java.util.HashMap;
import java.util.Map;
public class InMemoryOrderRepository implements OrderRepository {
    private final Map<Integer, Order> store = new HashMap<>();
    @Override
    public void save(Order order) { store.put(order.getId(), order); }
    public int size() { return store.size(); }
}
