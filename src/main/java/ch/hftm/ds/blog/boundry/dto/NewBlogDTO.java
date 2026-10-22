package ch.hftm.ds.blog.boundry.dto;

import ch.hftm.ds.blog.entity.Blog;
import ch.hftm.ds.blog.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request-DTO für {@code POST /blogs}.
 *
 * Enthält nur die Felder, die ein Client beim Anlegen eines Blogs setzen darf.
 * Die {@code id} wird bewusst nicht entgegengenommen, sie wird vom Server
 * generiert. Der Autor wird über die {@code authorId} referenziert statt über
 * ein verschachteltes User-Objekt.
 */
public class NewBlogDTO {

    @NotBlank(message = "title must not be blank")
    private String title;

    @NotBlank(message = "content must not be blank")
    private String content;

    @NotNull(message = "authorId must not be null")
    private Long authorId;

    public NewBlogDTO() {
    }

    public NewBlogDTO(String title, String content, Long authorId) {
        this.title = title;
        this.content = content;
        this.authorId = authorId;
    }

    /** Mapping DTO -> Entity. Der Autor wird vom Aufrufer aufgelöst. */
    public Blog toEntity(User author) {
        return new Blog(title, content, author);
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
}
