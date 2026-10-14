package com.community.ecommerce.repository;

import com.community.ecommerce.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByUserIdOrderByCreateTimeDesc(String userId);
    List<Order> findByUserIdAndStatusOrderByCreateTimeDesc(String userId, String status);
    Optional<Order> findByOrderNo(String orderNo);
    List<Order> findByStatus(String status);
}
