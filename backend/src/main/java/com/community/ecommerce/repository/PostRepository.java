package com.community.ecommerce.repository;

import com.community.ecommerce.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, String> {
    Page<Post> findAllByOrderByCreateTimeDesc(Pageable pageable);
}
