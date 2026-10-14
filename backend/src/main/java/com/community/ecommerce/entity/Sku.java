package com.community.ecommerce.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "sku")
public class Sku {
    @Id
    @Column(length = 32)
    private String id;

    /** 与 Goods 的 @OneToMany @JoinColumn(name="goods_id") 共享列，本字段只读 */
    @Column(name = "goods_id", insertable = false, updatable = false, length = 32)
    private String goodsId;

    @Column(length = 500)
    private String specValues; // JSON: {"颜色":"曜石黑"}

    private Double price;
    private Integer stock;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getGoodsId() { return goodsId; }
    public String getSpecValues() { return specValues; }
    public void setSpecValues(String specValues) { this.specValues = specValues; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
}
