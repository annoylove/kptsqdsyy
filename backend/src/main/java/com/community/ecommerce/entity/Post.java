package com.community.ecommerce.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "post")
public class Post {
    @Id
    @Column(length = 32)
    private String id;
    private String authorName;
    private String authorAvatar;
    private String title;

    @Column(length = 4000)
    private String content;

    @Column(length = 4000)
    private String images; // JSON 数组

    private Integer likes;
    private Integer comments;
    private Long createTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorAvatar() { return authorAvatar; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }
    public Integer getLikes() { return likes; }
    public void setLikes(Integer likes) { this.likes = likes; }
    public Integer getComments() { return comments; }
    public void setComments(Integer comments) { this.comments = comments; }
    public Long getCreateTime() { return createTime; }
    public void setCreateTime(Long createTime) { this.createTime = createTime; }
}
