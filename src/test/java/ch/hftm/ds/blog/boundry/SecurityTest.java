package ch.hftm.ds.blog.boundry;

import static io.restassured.RestAssured.given;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import ch.hftm.ds.blog.control.BlogService;
import ch.hftm.ds.blog.control.UserService;
import ch.hftm.ds.blog.entity.Blog;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;

/**
 * Prüft das Berechtigungskonzept: Lesen ist öffentlich, Schreiben braucht
 * die Rolle {@code user} oder {@code admin}, die Benutzerverwaltung ist nur
 * für {@code admin}. Einen Blog ändern oder löschen darf nur sein Autor oder
 * ein Admin.
 */
@QuarkusTest
public class SecurityTest {

    @Inject
    UserService userService;

    @Inject
    BlogService blogService;

    private static final String NEW_BLOG = "{\"title\":\"t\",\"content\":\"c\"}";
    private static final String UPDATE_BLOG = "{\"title\":\"changed\",\"content\":\"changed\"}";

    @Test
    void readingBlogsAnonymously_isAllowed() {
        given()
                .when().get("/blogs")
                .then()
                .statusCode(200);
    }

    @Test
    void creatingBlogAnonymously_returnsUnauthorized() {
        given()
                .contentType("application/json")
                .body(NEW_BLOG)
                .when().post("/blogs")
                .then()
                .statusCode(401);
    }

    @Test
    void deletingCommentAnonymously_returnsUnauthorized() {
        given()
                .when().delete("/blogs/{blogId}/comments/{commentId}", 1, 1)
                .then()
                .statusCode(401);
    }

    @Test
    void listingUsersAnonymously_returnsUnauthorized() {
        given()
                .when().get("/users")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "bob", roles = "user")
    void listingUsersAsUser_returnsForbidden() {
        given()
                .when().get("/users")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "bob", roles = "user")
    void creatingUserAsUser_returnsForbidden() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"x\"}")
                .when().post("/users")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "bob", roles = "user")
    void creatingBlogAsUser_setsUserAsAuthor() {
        given()
                .contentType("application/json")
                .body(NEW_BLOG)
                .when().post("/blogs")
                .then()
                .statusCode(201)
                .body("authorUsername", equalTo("bob"));
    }

    @Test
    @TestSecurity(user = "bob", roles = "user")
    void updatingOwnBlogAsUser_isAllowed() {
        Long blogId = createBlogOf("bob");
        given()
                .contentType("application/json")
                .body(UPDATE_BLOG)
                .when().put("/blogs/{id}", blogId)
                .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "bob", roles = "user")
    void updatingForeignBlogAsUser_returnsForbidden() {
        Long blogId = createBlogOf("alice");
        given()
                .contentType("application/json")
                .body(UPDATE_BLOG)
                .when().put("/blogs/{id}", blogId)
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "bob", roles = "user")
    void deletingForeignBlogAsUser_returnsForbidden() {
        Long blogId = createBlogOf("alice");
        given()
                .when().delete("/blogs/{id}", blogId)
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "alice", roles = { "admin", "user" })
    void updatingForeignBlogAsAdmin_isAllowed() {
        Long blogId = createBlogOf("bob");
        given()
                .contentType("application/json")
                .body(UPDATE_BLOG)
                .when().put("/blogs/{id}", blogId)
                .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "alice", roles = { "admin", "user" })
    void deletingForeignBlogAsAdmin_isAllowed() {
        Long blogId = createBlogOf("bob");
        given()
                .when().delete("/blogs/{id}", blogId)
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "alice", roles = { "admin", "user" })
    void listingUsersAsAdmin_isAllowed() {
        given()
                .when().get("/users")
                .then()
                .statusCode(200);
    }

    /** Legt einen Blog direkt über den Service an, unabhängig von der Test-Identität. */
    private Long createBlogOf(String username) {
        Blog blog = new Blog("Blog of " + username, "content", userService.getOrCreateUser(username));
        blogService.addBlog(blog);
        return blog.getId();
    }
}
