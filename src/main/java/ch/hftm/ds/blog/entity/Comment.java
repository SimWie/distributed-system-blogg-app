package ch.hftm.ds.blog.entity;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Comment {
    @Id
    @GeneratedValue
    private Long id;

    private String content;

    @JsonbTransient
    @ManyToOne
    private Blog blog;

    @ManyToOne
    private User author;

    public Comment() {
    }

    public Comment(String content, Blog blog, User author) {
        this.content = content;
        this.blog = blog;
        this.author = author;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Blog getBlog() {
        return blog;
    }

    public void setBlog(Blog blog) {
        this.blog = blog;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }
}
