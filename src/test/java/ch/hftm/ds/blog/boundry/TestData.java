package ch.hftm.ds.blog.boundry;

import static io.restassured.RestAssured.given;

import java.util.UUID;

/**
 * Hilfsmethoden, um Testdaten über die REST-API anzulegen. Jeder Aufruf
 * verwendet eindeutige Namen, damit Tests sich nicht gegenseitig stören.
 */
final class TestData {

    private TestData() {
    }

    static Long createUser() {
        String username = "test-user-" + UUID.randomUUID();
        return given()
                .contentType("application/json")
                .body("{\"username\":\"" + username + "\"}")
                .when().post("/users")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    static Long createBlog(String title, String content, Long authorId) {
        String body = "{\"title\":\"" + title + "\",\"content\":\"" + content + "\","
                + "\"authorId\":" + authorId + "}";
        return given()
                .contentType("application/json")
                .body(body)
                .when().post("/blogs")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }
}
