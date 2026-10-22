package ch.hftm.ds.blog.boundry.dto;

import ch.hftm.ds.blog.entity.Comment;

/**
 * Response-DTO für Kommentare.
 *
 * Gibt {@code id}, {@code content}, {@code blogId} sowie {@code authorId}
 * und {@code authorUsername} frei. Der Blog selbst wird nicht eingebettet.
 */
public class CommentDTO {

    private Long id;
    private String content;
    private Long blogId;
    private Long authorId;
    private String authorUsername;

    public CommentDTO() {
    }

    /** Mapping Entity -> DTO. */
    public static CommentDTO from(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.id = comment.getId();
        dto.content = comment.getContent();
        if (comment.getBlog() != null) {
            dto.blogId = comment.getBlog().getId();
        }
        if (comment.getAuthor() != null) {
            dto.authorId = comment.getAuthor().getId();
            dto.authorUsername = comment.getAuthor().getUsername();
        }
        return dto;
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

    public Long getBlogId() {
        return blogId;
    }

    public void setBlogId(Long blogId) {
        this.blogId = blogId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
    }
}
