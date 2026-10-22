package ch.hftm.ds.blog.boundry.dto;

import ch.hftm.ds.blog.entity.Blog;
import ch.hftm.ds.blog.entity.Comment;
import ch.hftm.ds.blog.entity.User;
import jakarta.validation.constraints.NotBlank;

/**
 * Request-DTO für {@code POST /blogs/{blogId}/comments?userId={id}}.
 *
 * Der Client liefert nur den Text. Blog (aus dem Pfad) und Autor (aus dem
 * Query-Parameter {@code userId}) werden serverseitig aufgelöst.
 */
public class NewCommentDTO {

    @NotBlank(message = "content must not be blank")
    private String content;

    public NewCommentDTO() {
    }

    public NewCommentDTO(String content) {
        this.content = content;
    }

    /** Mapping DTO -> Entity. Blog und Autor werden vom Aufrufer aufgelöst. */
    public Comment toEntity(Blog blog, User author) {
        return new Comment(content, blog, author);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
