package com.community.ecommerce.repository;

import com.community.ecommerce.entity.Goods;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoodsRepository extends JpaRepository<Goods, String> {
    List<Goods> findByStatus(String status);
    List<Goods> findByStatusAndTitleContaining(String status, String keyword);
    List<Goods> findByShopIdAndStatus(String shopId, String status);
}
