package vn.devpro.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.devpro.marketplace.entity.Order;
import vn.devpro.marketplace.entity.OrderItem;
import vn.devpro.marketplace.entity.Product;
import vn.devpro.marketplace.entity.User;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    boolean existsByOrderUserAndProductAndOrderStatus(User user,
                                                       Product product,
                                                       Order.OrderStatus status);

    List<OrderItem> findByOrder(Order order);
}
