package ch.hftm.ds.blog.boundry;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

/**
 * REST-Level-Tests für die verschachtelte Kommentar-Ressource
 * ({@code /blogs/{blogId}/comments}) mit Request-DTO {@code NewCommentDTO}
 * und Response-DTO {@code CommentDTO}.
 */
@QuarkusTest
@TestSecurity(user = "alice", roles = { "admin", "user" })
public class CommentResourceTest {

    private Long userId;
    private Long blogId;

    @BeforeEach
    void createUserAndBlog() {
        userId = TestData.createUser();
        blogId = TestData.createBlog("Blog for comments", "content");
    }

    @Test
    void addingComment_returnsCreatedWithDtoFields() {
        given()
                .contentType("application/json")
                .queryParam("userId", userId)
                .body("{\"content\":\"Guter Post!\"}")
                .when().post("/blogs/{blogId}/comments", blogId)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("content", equalTo("Guter Post!"))
                .body("blogId", equalTo(blogId.intValue()))
                .body("authorId", equalTo(userId.intValue()))
                .body("authorUsername", notNullValue())
                // Weder der Blog noch das User-Objekt werden verschachtelt ausgeliefert.
                .body("$", not(hasKey("blog")))
                .body("$", not(hasKey("author")));
    }

    @Test
    void gettingComments_returnsCommentsOfBlog() {
        given()
                .contentType("application/json")
                .queryParam("userId", userId)
                .body("{\"content\":\"Ein Kommentar\"}")
                .when().post("/blogs/{blogId}/comments", blogId)
                .then()
                .statusCode(201);

        given()
                .when().get("/blogs/{blogId}/comments", blogId)
                .then()
                .statusCode(200)
                .body("content", hasItem("Ein Kommentar"));
    }

    @Test
    void gettingCommentsOfUnknownBlog_returnsNotFoundWithMessage() {
        given()
                .when().get("/blogs/{blogId}/comments", 999_999_999L)
                .then()
                .statusCode(404)
                .body("message", containsString("not found"));
    }

    @Test
    void addingCommentWithoutUserId_returnsBadRequest() {
        given()
                .contentType("application/json")
                .body("{\"content\":\"Kein Autor\"}")
                .when().post("/blogs/{blogId}/comments", blogId)
                .then()
                .statusCode(400)
                .body("message", containsString("userId"));
    }

    @Test
    void addingCommentWithUnknownUser_returnsNotFoundWithMessage() {
        given()
                .contentType("application/json")
                .queryParam("userId", 999_999_999L)
                .body("{\"content\":\"Unbekannter Autor\"}")
                .when().post("/blogs/{blogId}/comments", blogId)
                .then()
                .statusCode(404)
                .body("message", containsString("User with id 999999999 not found"));
    }

    @Test
    void addingCommentToUnknownBlog_returnsNotFoundWithMessage() {
        given()
                .contentType("application/json")
                .queryParam("userId", userId)
                .body("{\"content\":\"Unbekannter Blog\"}")
                .when().post("/blogs/{blogId}/comments", 999_999_999L)
                .then()
                .statusCode(404)
                .body("message", containsString("Blog with id 999999999 not found"));
    }

    @Test
    void addingCommentWithBlankContent_returnsBadRequest() {
        given()
                .contentType("application/json")
                .queryParam("userId", userId)
                .body("{\"content\":\"\"}")
                .when().post("/blogs/{blogId}/comments", blogId)
                .then()
                .statusCode(400)
                .body("message", containsString("content"));
    }

    @Test
    void deletingComment_returnsNoContentAndThenNotFound() {
        Long commentId = given()
                .contentType("application/json")
                .queryParam("userId", userId)
                .body("{\"content\":\"Wird gelöscht\"}")
                .when().post("/blogs/{blogId}/comments", blogId)
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .when().delete("/blogs/{blogId}/comments/{commentId}", blogId, commentId)
                .then()
                .statusCode(204);

        given()
                .when().delete("/blogs/{blogId}/comments/{commentId}", blogId, commentId)
                .then()
                .statusCode(404)
                .body("message", containsString("not found"));
    }
}
