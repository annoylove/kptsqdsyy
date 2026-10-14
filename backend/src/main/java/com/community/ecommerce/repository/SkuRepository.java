package com.community.ecommerce.repository;

import com.community.ecommerce.entity.Sku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SkuRepository extends JpaRepository<Sku, String> {
    List<Sku> findByGoodsId(String goodsId);

    /**
     * 原子条件扣减库存：UPDATE ... WHERE stock >= qty。
     * 并发下由数据库行锁保证"检查+扣减"不可分割，返回 0 即库存不足，杜绝超卖。
     */
    @Modifying
    @Query("UPDATE Sku s SET s.stock = s.stock - :qty WHERE s.id = :skuId AND s.stock >= :qty")
    int deductStock(@Param("skuId") String skuId, @Param("qty") int qty);

    /** 原子回补库存（取消/退款），避免读改写竞态 */
    @Modifying
    @Query("UPDATE Sku s SET s.stock = s.stock + :qty WHERE s.id = :skuId")
    int restoreStock(@Param("skuId") String skuId, @Param("qty") int qty);
}
