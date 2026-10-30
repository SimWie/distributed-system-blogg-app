package ch.hftm.ds.blog.boundry.dto;

import ch.hftm.ds.blog.entity.Blog;
import ch.hftm.ds.blog.entity.User;
import jakarta.validation.constraints.NotBlank;

/**
 * Request-DTO für {@code POST /blogs}.
 *
 * Enthält nur die Felder, die ein Client beim Anlegen eines Blogs setzen darf.
 * Die {@code id} wird bewusst nicht entgegengenommen, sie wird vom Server
 * generiert. Auch der Autor wird nicht vom Client angegeben, sondern aus dem
 * Access-Token des angemeldeten Benutzers bestimmt.
 */
public class NewBlogDTO {

    @NotBlank(message = "title must not be blank")
    private String title;

    @NotBlank(message = "content must not be blank")
    private String content;

    public NewBlogDTO() {
    }

    public NewBlogDTO(String title, String content) {
        this.title = title;
        this.content = content;
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
}
