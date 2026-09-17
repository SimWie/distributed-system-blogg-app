package ch.hftm.ds.blog.boundry;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * REST-Level-Tests für den Blog-Endpunkt.
 *
 * Deckt die Mindestanforderungen aus dem REST-Auftrag ab:
 * - PUT-Request zur Aktualisierung von Daten (siehe {@link #updatingABlog_returnsUpdatedFields()})
 * - Query-Parameter für Suche (siehe {@link #searchQueryParam_filtersBlogsByTitle()})
 * - Aussagekräftige Fehler-Responses (siehe {@link #gettingUnknownBlog_returnsNotFoundWithMessage()})
 * - Bean Validation eines ungültigen Requests (siehe {@link #creatingBlogWithBlankTitle_returnsBadRequest()})
 */
@QuarkusTest
public class BlogResourceTest {

    private static Long testAuthorId;

    @BeforeAll
    static void createTestAuthor() {
        // RestAssured's port is normally auto-synced by Quarkus before each
        // @Test method, but that sync runs too late for a static @BeforeAll -
        // set it explicitly here so the very first request doesn't hit the
        // wrong (default) port.
        RestAssured.port = Integer.getInteger("quarkus.http.test-port", 8081);

        String username = "blog-test-user-" + UUID.randomUUID();
        testAuthorId = given()
                .contentType("application/json")
                .body("{\"username\":\"" + username + "\"}")
                .when().post("/users")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    @Test
    void gettingAllBlogs_returnsOk() {
        given()
                .when().get("/blogs")
                .then()
                .statusCode(200);
    }

    @Test
    void gettingUnknownBlog_returnsNotFoundWithMessage() {
        given()
                .when().get("/blogs/{id}", 999_999_999L)
                .then()
                .statusCode(404)
                .body("message", containsString("not found"));
    }

    @Test
    void creatingBlogWithBlankTitle_returnsBadRequest() {
        String body = "{\"title\":\"\",\"content\":\"some content\"}";

        given()
                .contentType("application/json")
                .body(body)
                .when().post("/blogs")
                .then()
                // Bean Validation (@NotBlank auf Blog.title) schlägt fehl.
                .statusCode(400)
                .body("message", containsString("title"));
    }

    @Test
    void creatingAndUpdatingABlog_returnsUpdatedFields() {
        String createBody = "{\"title\":\"Original Title\",\"content\":\"Original content\","
                + "\"author\":{\"id\":" + testAuthorId + "}}";

        Long blogId = given()
                .contentType("application/json")
                .body(createBody)
                .when().post("/blogs")
                .then()
                .statusCode(201)
                .body("title", equalTo("Original Title"))
                .extract().jsonPath().getLong("id");

        String updateBody = "{\"title\":\"Updated Title\",\"content\":\"Updated content\"}";

        given()
                .contentType("application/json")
                .body(updateBody)
                .when().put("/blogs/{id}", blogId)
                .then()
                .statusCode(200)
                .body("title", equalTo("Updated Title"))
                .body("content", equalTo("Updated content"));

        given()
                .when().get("/blogs/{id}", blogId)
                .then()
                .statusCode(200)
                .body("title", equalTo("Updated Title"));
    }

    @Test
    void updatingUnknownBlog_returnsNotFoundWithMessage() {
        String updateBody = "{\"title\":\"Doesn't matter\",\"content\":\"Doesn't matter\"}";

        given()
                .contentType("application/json")
                .body(updateBody)
                .when().put("/blogs/{id}", 999_999_999L)
                .then()
                .statusCode(404)
                .body("message", containsString("not found"));
    }

    @Test
    void searchQueryParam_filtersBlogsByTitle() {
        String uniqueTerm = "SearchTerm" + UUID.randomUUID().toString().substring(0, 8);
        String matchingTitle = uniqueTerm + " - matching blog";
        String otherTitle = "Unrelated blog " + UUID.randomUUID();

        createBlog(matchingTitle, "content", testAuthorId);
        createBlog(otherTitle, "content", testAuthorId);

        List<String> titles = given()
                .queryParam("search", uniqueTerm)
                .when().get("/blogs")
                .then()
                .statusCode(200)
                .extract().jsonPath().getList("title", String.class);

        assertTrue(titles.contains(matchingTitle), "Suchergebnis sollte den passenden Blog enthalten");
        assertTrue(titles.stream().noneMatch(t -> t.equals(otherTitle)),
                "Suchergebnis sollte nicht-passende Blogs ausschliessen");
    }

    private void createBlog(String title, String content, Long authorId) {
        String body = "{\"title\":\"" + title + "\",\"content\":\"" + content + "\","
                + "\"author\":{\"id\":" + authorId + "}}";
        given()
                .contentType("application/json")
                .body(body)
                .when().post("/blogs")
                .then()
                .statusCode(201)
                .body("id", notNullValue());
    }
}
