package food.delivery.repository;
import food.delivery.model.Order;
public interface OrderRepository {
    void save(Order order);
}
