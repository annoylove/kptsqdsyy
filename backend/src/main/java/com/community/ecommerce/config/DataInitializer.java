package com.community.ecommerce.config;

import com.community.ecommerce.entity.*;
import com.community.ecommerce.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** 种子数据：与前端 mock/data.js 保持一致 */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(ShopRepository shops, GoodsRepository goods, PostRepository posts) {
        return args -> {
            // MySQL 持久化库：已有数据则跳过，避免覆盖业务数据（H2 内存库每次启动都是空的，正常插入）
            if (goods.count() > 0 && posts.count() > 0) {
                System.out.println("[DataInitializer] 数据库已有数据，跳过种子数据初始化");
                return;
            }

            shops.saveAll(List.of(
                    shop("S001", "优选数码旗舰店", 4.8),
                    shop("S002", "轻食生活馆", 4.6),
                    shop("S003", "山野户外专营店", 4.9)));

            goods.saveAll(List.of(
                    g001(), g002(), g003(), g004(), g005(), g006()));

            long now = System.currentTimeMillis();
            posts.saveAll(List.of(
                    post("P001", "露营狂人", "a1", "周末露营装备清单分享",
                            "这次带上了新买的帐篷和炉具，山顶日出太美了！附上清单：帐篷、防潮垫、炉头、气罐、折叠椅……",
                            imgs(4, "p00", "/600/800"), 328, 45, now - 2 * 3600_000L),
                    post("P002", "数码小白", "a2", "降噪耳机横评：千元内谁最强",
                            "实测了 5 款热门降噪耳机，从佩戴舒适度到降噪深度逐项打分，结论放在最后，性价比之王居然是它……",
                            imgs(3, "p01", "/600/600"), 512, 89, now - 5 * 3600_000L),
                    post("P003", "轻食日记", "a3", "一周减脂餐打卡 Day3",
                            "今天的午餐是鸡胸肉沙拉配冷萃咖啡，0糖0脂无负担，坚持第 3 天，已经掉了 1.2kg！",
                            "[\"https://picsum.photos/seed/p020/600/900\"]", 189, 23, now - 8 * 3600_000L),
                    post("P004", "徒步老王", "a4", "冲锋衣到底要不要买三合一？",
                            "穿了十年的冲锋衣，从单层到三合一都试过。结论：南方单穿内胆，北方必须三合一，听我的没错。",
                            imgs(6, "p03", "/600/750"), 764, 132, now - 26 * 3600_000L),
                    post("P005", "手冲咖啡君", "a5", "冷萃 vs 手冲，口感对比",
                            "同一支豆子，两种做法，风味差异比想象的大。冷萃更甜润，手冲香气更立体，你们站哪边？",
                            imgs(2, "p04", "/600/600"), 233, 56, now - 30 * 3600_000L),
                    post("P006", "数码小白", "a2", "智能手表一个月真实体验",
                            "心率、血氧、睡眠监测都测了，准确度对得起价格，就是续航比官方宣传少了两天……",
                            imgs(5, "p05", "/600/800"), 421, 67, now - 48 * 3600_000L)));
        };
    }

    private static Shop shop(String id, String name, double rating) {
        Shop s = new Shop();
        s.setId(id);
        s.setName(name);
        s.setRating(rating);
        return s;
    }

    private static Sku sku(String id, String specValues, double price, int stock) {
        Sku s = new Sku();
        s.setId(id);
        s.setSpecValues(specValues);
        s.setPrice(price);
        s.setStock(stock);
        return s;
    }

    private static Goods goods(String id, String shopId, String title, String cover, String images,
                               double price, int sales, String status, String specs, List<Sku> skus) {
        Goods g = new Goods();
        g.setId(id);
        g.setShopId(shopId);
        g.setTitle(title);
        g.setCover(cover);
        g.setImages(images);
        g.setPrice(price);
        g.setSales(sales);
        g.setStatus(status);
        g.setSpecs(specs);
        g.setSkus(skus);
        return g;
    }

    private static Goods g001() {
        return goods("G001", "S001", "无线降噪蓝牙耳机 Pro 2 长续航运动跑步",
                "https://picsum.photos/seed/g001/400/400",
                "[\"https://picsum.photos/seed/g001/750/750\",\"https://picsum.photos/seed/g001b/750/750\"]",
                299, 1200, "on",
                "[{\"name\":\"颜色\",\"values\":[\"曜石黑\",\"珍珠白\"]},{\"name\":\"型号\",\"values\":[\"标准版\",\"降噪版\"]}]",
                List.of(
                        sku("G001-A", "{\"颜色\":\"曜石黑\",\"型号\":\"标准版\"}", 299, 50),
                        sku("G001-B", "{\"颜色\":\"曜石黑\",\"型号\":\"降噪版\"}", 399, 20),
                        sku("G001-C", "{\"颜色\":\"珍珠白\",\"型号\":\"标准版\"}", 299, 8),
                        sku("G001-D", "{\"颜色\":\"珍珠白\",\"型号\":\"降噪版\"}", 399, 0)));
    }

    private static Goods g002() {
        return goods("G002", "S001", "智能手表 心率血氧监测 防水运动手环",
                "https://picsum.photos/seed/g002/400/400",
                "[\"https://picsum.photos/seed/g002/750/750\"]",
                599, 860, "on",
                "[{\"name\":\"颜色\",\"values\":[\"午夜黑\",\"星光银\"]},{\"name\":\"表带\",\"values\":[\"硅胶\",\"金属\"]}]",
                List.of(
                        sku("G002-A", "{\"颜色\":\"午夜黑\",\"表带\":\"硅胶\"}", 599, 30),
                        sku("G002-B", "{\"颜色\":\"午夜黑\",\"表带\":\"金属\"}", 699, 12),
                        sku("G002-C", "{\"颜色\":\"星光银\",\"表带\":\"硅胶\"}", 599, 15),
                        sku("G002-D", "{\"颜色\":\"星光银\",\"表带\":\"金属\"}", 699, 3)));
    }

    private static Goods g003() {
        return goods("G003", "S002", "每日坚果混合装 30包 孕妇儿童零食大礼包",
                "https://picsum.photos/seed/g003/400/400",
                "[\"https://picsum.photos/seed/g003/750/750\"]",
                89, 5600, "on",
                "[{\"name\":\"规格\",\"values\":[\"7袋体验装\",\"15袋中盒\",\"30袋大箱\"]}]",
                List.of(
                        sku("G003-A", "{\"规格\":\"7袋体验装\"}", 29, 200),
                        sku("G003-B", "{\"规格\":\"15袋中盒\"}", 59, 120),
                        sku("G003-C", "{\"规格\":\"30袋大箱\"}", 89, 60)));
    }

    private static Goods g004() {
        // 已下架 -> 购物车失效商品演示
        return goods("G004", "S002", "冷萃咖啡液 0糖0脂 美式浓缩 12杯装",
                "https://picsum.photos/seed/g004/400/400",
                "[\"https://picsum.photos/seed/g004/750/750\"]",
                68, 2300, "off",
                "[{\"name\":\"口味\",\"values\":[\"原味\",\"榛果\"]}]",
                List.of(
                        sku("G004-A", "{\"口味\":\"原味\"}", 68, 40),
                        sku("G004-B", "{\"口味\":\"榛果\"}", 72, 25)));
    }

    private static Goods g005() {
        return goods("G005", "S003", "户外冲锋衣 三合一防水防风 登山徒步外套",
                "https://picsum.photos/seed/g005/400/400",
                "[\"https://picsum.photos/seed/g005/750/750\"]",
                459, 430, "on",
                "[{\"name\":\"颜色\",\"values\":[\"军绿\",\"藏青\",\"烈焰橙\"]},{\"name\":\"尺码\",\"values\":[\"M\",\"L\",\"XL\",\"XXL\"]}]",
                List.of(
                        sku("G005-A", "{\"颜色\":\"军绿\",\"尺码\":\"M\"}", 459, 10),
                        sku("G005-B", "{\"颜色\":\"军绿\",\"尺码\":\"L\"}", 459, 6),
                        sku("G005-C", "{\"颜色\":\"军绿\",\"尺码\":\"XL\"}", 459, 0),
                        sku("G005-D", "{\"颜色\":\"军绿\",\"尺码\":\"XXL\"}", 489, 2),
                        sku("G005-E", "{\"颜色\":\"藏青\",\"尺码\":\"M\"}", 459, 8),
                        sku("G005-F", "{\"颜色\":\"藏青\",\"尺码\":\"L\"}", 459, 4),
                        sku("G005-G", "{\"颜色\":\"藏青\",\"尺码\":\"XL\"}", 459, 9),
                        sku("G005-H", "{\"颜色\":\"藏青\",\"尺码\":\"XXL\"}", 489, 0),
                        sku("G005-I", "{\"颜色\":\"烈焰橙\",\"尺码\":\"M\"}", 499, 3),
                        sku("G005-J", "{\"颜色\":\"烈焰橙\",\"尺码\":\"L\"}", 499, 5),
                        sku("G005-K", "{\"颜色\":\"烈焰橙\",\"尺码\":\"XL\"}", 499, 1),
                        sku("G005-L", "{\"颜色\":\"烈焰橙\",\"尺码\":\"XXL\"}", 529, 0)));
    }

    private static Goods g006() {
        return goods("G006", "S003", "轻量化露营帐篷 双人防暴雨 速开野外装备",
                "https://picsum.photos/seed/g006/400/400",
                "[\"https://picsum.photos/seed/g006/750/750\"]",
                328, 190, "on",
                "[{\"name\":\"款式\",\"values\":[\"双人款\",\"四人款\"]}]",
                List.of(
                        sku("G006-A", "{\"款式\":\"双人款\"}", 328, 18),
                        sku("G006-B", "{\"款式\":\"四人款\"}", 468, 5)));
    }

    private static String imgs(int n, String seedPrefix, String size) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"https://picsum.photos/seed/").append(seedPrefix).append(i).append(size).append("\"");
        }
        return sb.append("]").toString();
    }

    private static Post post(String id, String author, String seed, String title, String content,
                             String images, int likes, int comments, long createTime) {
        Post p = new Post();
        p.setId(id);
        p.setAuthorName(author);
        p.setAuthorAvatar("https://picsum.photos/seed/" + seed + "/100/100");
        p.setTitle(title);
        p.setContent(content);
        p.setImages(images);
        p.setLikes(likes);
        p.setComments(comments);
        p.setCreateTime(createTime);
        return p;
    }
}
