package com.community.ecommerce.controller;

import com.community.ecommerce.common.ApiResponse;
import com.community.ecommerce.common.BizException;
import com.community.ecommerce.common.CurrentUser;
import com.community.ecommerce.entity.Post;
import com.community.ecommerce.repository.PostRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 社区接口
 * GET  /api/post/list?page=1&size=10 -> {list, hasMore, total}
 * GET  /api/post/detail?id=          -> post
 * POST /api/post/publish (需登录)     -> post
 * POST /api/post/like {id, liked}    -> {likes, liked}
 */
@RestController
@RequestMapping("/api/post")
public class PostController {

    private final PostRepository postRepo;
    private final ObjectMapper mapper;

    /** 发布防刷：userId -> 上次成功发布时间戳（3 秒内拒绝再次发布） */
    private final Map<String, Long> lastPublishAt = new ConcurrentHashMap<>();
    private static final long PUBLISH_INTERVAL_MS = 3000;

    public PostController(PostRepository postRepo, ObjectMapper mapper) {
        this.postRepo = postRepo;
        this.mapper = mapper;
    }

    @GetMapping("/list")
    public ApiResponse list(@RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "10") int size) {
        Page<Post> p = postRepo.findAllByOrderByCreateTimeDesc(PageRequest.of(Math.max(0, page - 1), size));
        List<Map<String, Object>> list = p.getContent().stream().map(this::toDto).toList();
        return ApiResponse.ok(ApiResponse.map(
                "list", list,
                "hasMore", p.hasNext(),
                "total", p.getTotalElements()));
    }

    @GetMapping("/detail")
    public ApiResponse detail(@RequestParam String id) {
        Post post = postRepo.findById(id).orElseThrow(() -> new BizException(404, "帖子不存在"));
        return ApiResponse.ok(toDto(post));
    }

    @PostMapping("/publish")
    public ApiResponse publish(@RequestBody Map<String, Object> body) {
        String userId = CurrentUser.id();
        // 发布防刷：3 秒内同一用户仅允许发布一次
        long now = System.currentTimeMillis();
        Long last = lastPublishAt.get(userId);
        if (last != null && now - last < PUBLISH_INTERVAL_MS) {
            throw new BizException(429, "发布过于频繁，请稍后再试");
        }
        lastPublishAt.put(userId, now);
        Post post = new Post();
        post.setId("P" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        post.setAuthorName("山间来客");
        post.setAuthorAvatar("https://picsum.photos/seed/u001/200/200");
        post.setTitle(String.valueOf(body.getOrDefault("title", "无标题动态")));
        post.setContent(String.valueOf(body.getOrDefault("content", "")));
        Object images = body.getOrDefault("images", List.of());
        try {
            post.setImages(mapper.writeValueAsString(images));
        } catch (Exception e) {
            post.setImages("[]");
        }
        post.setLikes(0);
        post.setComments(0);
        post.setCreateTime(System.currentTimeMillis());
        postRepo.save(post);
        return ApiResponse.ok(toDto(post));
    }

    @PostMapping("/like")
    public ApiResponse like(@RequestBody Map<String, Object> body) {
        Post post = postRepo.findById(String.valueOf(body.get("id")))
                .orElseThrow(() -> new BizException(404, "帖子不存在"));
        boolean liked = Boolean.TRUE.equals(body.get("liked"));
        post.setLikes(post.getLikes() + (liked ? 1 : -1));
        postRepo.save(post);
        return ApiResponse.ok(ApiResponse.map("likes", post.getLikes(), "liked", liked));
    }

    private Map<String, Object> toDto(Post p) {
        List<String> images;
        try {
            images = mapper.readValue(p.getImages(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            images = List.of();
        }
        return ApiResponse.map(
                "id", p.getId(),
                "author", ApiResponse.map("nickname", p.getAuthorName(), "avatar", p.getAuthorAvatar()),
                "title", p.getTitle(),
                "content", p.getContent(),
                "images", images,
                "likes", p.getLikes(),
                "comments", p.getComments(),
                "createTime", p.getCreateTime(),
                "liked", false);
    }
}
