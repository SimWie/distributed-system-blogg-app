package ch.hftm.ds.blog.boundry.dto;

import ch.hftm.ds.blog.entity.Blog;

/**
 * Response-DTO für Blogs.
 *
 * Gibt nur {@code id}, {@code title}, {@code content} sowie {@code authorId}
 * und {@code authorUsername} frei. Die Kommentar-Liste und das vollständige
 * User-Objekt der Entity werden nicht ausgeliefert.
 */
public class BlogDTO {

    private Long id;
    private String title;
    private String content;
    private Long authorId;
    private String authorUsername;

    public BlogDTO() {
    }

    /** Mapping Entity -> DTO. */
    public static BlogDTO from(Blog blog) {
        BlogDTO dto = new BlogDTO();
        dto.id = blog.getId();
        dto.title = blog.getTitle();
        dto.content = blog.getContent();
        if (blog.getAuthor() != null) {
            dto.authorId = blog.getAuthor().getId();
            dto.authorUsername = blog.getAuthor().getUsername();
        }
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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
