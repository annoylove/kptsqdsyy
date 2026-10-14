package com.community.ecommerce.controller;

import com.community.ecommerce.common.ApiResponse;
import com.community.ecommerce.common.BizException;
import com.community.ecommerce.entity.Goods;
import com.community.ecommerce.entity.Shop;
import com.community.ecommerce.repository.GoodsRepository;
import com.community.ecommerce.repository.ShopRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商品接口
 * GET /api/goods/list?keyword=&shopId=  -> {list, total}
 * GET /api/goods/detail?id=G001         -> goods（含 specs JSON 解析与 skus）
 * GET /api/shop/list                    -> shops
 */
@RestController
@RequestMapping("/api")
public class GoodsController {

    private final GoodsRepository goodsRepo;
    private final ShopRepository shopRepo;
    private final ObjectMapper mapper;

    public GoodsController(GoodsRepository goodsRepo, ShopRepository shopRepo, ObjectMapper mapper) {
        this.goodsRepo = goodsRepo;
        this.shopRepo = shopRepo;
        this.mapper = mapper;
    }

    @GetMapping("/goods/list")
    public ApiResponse list(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String shopId) {
        List<Goods> list;
        if (keyword != null && !keyword.isBlank()) {
            list = goodsRepo.findByStatusAndTitleContaining("on", keyword);
        } else if (shopId != null && !shopId.isBlank()) {
            list = goodsRepo.findByShopIdAndStatus(shopId, "on");
        } else {
            list = goodsRepo.findByStatus("on");
        }
        return ApiResponse.ok(ApiResponse.map(
                "list", list.stream().map(this::toDto).toList(),
                "total", list.size()));
    }

    @GetMapping("/goods/detail")
    public ApiResponse detail(@RequestParam String id) {
        Goods g = goodsRepo.findById(id).orElseThrow(() -> new BizException(404, "商品不存在"));
        return ApiResponse.ok(toDto(g));
    }

    /** 实体 JSON 字符串字段解析为结构化对象（与前端 mock 数据结构一致） */
    private Map<String, Object> toDto(Goods g) {
        Object specs;
        try {
            specs = mapper.readValue(g.getSpecs(), new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            specs = List.of();
        }
        return ApiResponse.map(
                "id", g.getId(),
                "shopId", g.getShopId(),
                "title", g.getTitle(),
                "cover", g.getCover(),
                "images", parseJsonList(g.getImages()),
                "price", g.getPrice(),
                "sales", g.getSales(),
                "status", g.getStatus(),
                "specs", specs,
                "skus", g.getSkus().stream().map(s -> ApiResponse.map(
                        "id", s.getId(),
                        "specValues", parseSpecValues(s.getSpecValues()),
                        "price", s.getPrice(),
                        "stock", s.getStock()
                )).toList());
    }

    @GetMapping("/shop/list")
    public ApiResponse shops() {
        List<Shop> list = shopRepo.findAll();
        return ApiResponse.ok(list);
    }

    private Object parseJsonList(String json) {
        try {
            return mapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private Object parseSpecValues(String json) {
        try {
            return mapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
