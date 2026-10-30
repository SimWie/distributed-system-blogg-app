package ch.hftm.ds.blog.boundry;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

/**
 * REST-Level-Tests für den Blog-Endpunkt (Request-DTO {@code NewBlogDTO} /
 * {@code UpdateBlogDTO}, Response-DTO {@code BlogDTO}).
 *
 * Deckt die Mindestanforderungen aus dem REST-Auftrag ab:
 * - PUT-Request zur Aktualisierung von Daten (siehe {@link #creatingAndUpdatingABlog_returnsUpdatedFields()})
 * - Query-Parameter für Suche (siehe {@link #searchQueryParam_filtersBlogsByTitle()})
 * - Aussagekräftige Fehler-Responses (siehe {@link #gettingUnknownBlog_returnsNotFoundWithMessage()})
 * - Bean Validation eines ungültigen Requests (siehe {@link #creatingBlogWithBlankTitle_returnsBadRequest()})
 *
 * Zusätzlich wird der DTO-Vertrag geprüft: welche Felder ausgeliefert werden
 * und dass Client-seitig mitgeschickte Felder wie {@code id} ignoriert werden.
 */
@QuarkusTest
@TestSecurity(user = "alice", roles = { "admin", "user" })
public class BlogResourceTest {

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
                // Bean Validation (@NotBlank auf NewBlogDTO.title) schlägt fehl.
                .statusCode(400)
                .body("message", containsString("title"));
    }

    @Test
    void creatingBlog_takesAuthorFromToken() {
        given()
                .contentType("application/json")
                .body("{\"title\":\"Title\",\"content\":\"some content\"}")
                .when().post("/blogs")
                .then()
                .statusCode(201)
                .body("authorUsername", equalTo("alice"));
    }

    @Test
    void creatingBlog_ignoresClientSuppliedAuthorId() {
        Long otherUserId = TestData.createUser();

        given()
                .contentType("application/json")
                .body("{\"title\":\"Title\",\"content\":\"c\",\"authorId\":" + otherUserId + "}")
                .when().post("/blogs")
                .then()
                .statusCode(201)
                // Der Autor kommt aus dem Token, nicht aus dem Request.
                .body("authorId", not(equalTo(otherUserId.intValue())))
                .body("authorUsername", equalTo("alice"));
    }

    @Test
    void creatingBlog_returnsOnlyDtoFields() {
        String body = "{\"title\":\"DTO Title\",\"content\":\"DTO content\"}";

        given()
                .contentType("application/json")
                .body(body)
                .when().post("/blogs")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("title", equalTo("DTO Title"))
                .body("content", equalTo("DTO content"))
                .body("authorId", notNullValue())
                .body("authorUsername", equalTo("alice"))
                // Die Entity-Struktur (verschachtelter Autor, Kommentare) wird nicht ausgeliefert.
                .body("$", not(hasKey("author")))
                .body("$", not(hasKey("comments")));
    }

    @Test
    void creatingBlog_ignoresClientSuppliedId() {
        String body = "{\"id\":123456789,\"title\":\"Id Test\",\"content\":\"content\"}";

        Long createdId = given()
                .contentType("application/json")
                .body(body)
                .when().post("/blogs")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");

        // Das Request-DTO kennt kein id-Feld: die ID wird immer vom Server vergeben.
        assertNotEquals(123456789L, createdId);
    }

    @Test
    void creatingAndUpdatingABlog_returnsUpdatedFields() {
        String createBody = "{\"title\":\"Original Title\",\"content\":\"Original content\"}";

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
                .body("content", equalTo("Updated content"))
                // Der Autor bleibt beim Update unverändert.
                .body("authorUsername", equalTo("alice"));

        given()
                .when().get("/blogs/{id}", blogId)
                .then()
                .statusCode(200)
                .body("title", equalTo("Updated Title"));
    }

    @Test
    void updatingBlogWithBlankContent_returnsBadRequest() {
        Long blogId = TestData.createBlog("Title", "content");

        given()
                .contentType("application/json")
                .body("{\"title\":\"Title\",\"content\":\" \"}")
                .when().put("/blogs/{id}", blogId)
                .then()
                .statusCode(400)
                .body("message", containsString("content"));
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
    void deletingABlog_returnsNoContentAndThenNotFound() {
        Long blogId = TestData.createBlog("To delete", "content");

        given()
                .when().delete("/blogs/{id}", blogId)
                .then()
                .statusCode(204);

        given()
                .when().get("/blogs/{id}", blogId)
                .then()
                .statusCode(404);
    }

    @Test
    void searchQueryParam_filtersBlogsByTitle() {
        String uniqueTerm = "SearchTerm" + UUID.randomUUID().toString().substring(0, 8);
        String matchingTitle = uniqueTerm + " - matching blog";
        String otherTitle = "Unrelated blog " + UUID.randomUUID();

        TestData.createBlog(matchingTitle, "content");
        TestData.createBlog(otherTitle, "content");

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
}
