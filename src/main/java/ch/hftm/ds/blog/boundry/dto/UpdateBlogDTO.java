package ch.hftm.ds.blog.boundry.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request-DTO für {@code PUT /blogs/{id}}.
 *
 * Erlaubt nur die Änderung von Titel und Inhalt. Der Autor eines bestehenden
 * Blogs kann nicht über diesen Endpunkt geändert werden.
 */
public class UpdateBlogDTO {

    @NotBlank(message = "title must not be blank")
    private String title;

    @NotBlank(message = "content must not be blank")
    private String content;

    public UpdateBlogDTO() {
    }

    public UpdateBlogDTO(String title, String content) {
        this.title = title;
        this.content = content;
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
