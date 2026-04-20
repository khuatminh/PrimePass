package vn.devpro.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.devpro.marketplace.entity.Order;
import vn.devpro.marketplace.entity.User;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    List<Order> findByUserOrderByCreatedAtDesc(User user);

    Optional<Order> findByVnpayTxnRef(String vnpayTxnRef);

    List<Order> findByUserAndStatus(User user, Order.OrderStatus status);
}
