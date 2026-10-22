package ch.hftm.ds.blog.boundry;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Prüft die CORS-Konfiguration aus {@code application.properties}:
 * Nur der Angular-Dev-Server ({@code http://localhost:4200}) ist erlaubt,
 * andere Origins erhalten keine CORS-Freigabe.
 */
@QuarkusTest
public class CorsTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:4200";
    private static final String FOREIGN_ORIGIN = "https://evil.example.com";

    @Test
    void preflightFromAllowedOrigin_isAccepted() {
        given()
                .header("Origin", ALLOWED_ORIGIN)
                .header("Access-Control-Request-Method", "PUT")
                .header("Access-Control-Request-Headers", "content-type")
                .when().options("/blogs/1")
                .then()
                .header("Access-Control-Allow-Origin", equalTo(ALLOWED_ORIGIN))
                .header("Access-Control-Allow-Methods", containsString("PUT"));
    }

    @Test
    void preflightFromForeignOrigin_getsNoCorsHeaders() {
        given()
                .header("Origin", FOREIGN_ORIGIN)
                .header("Access-Control-Request-Method", "PUT")
                .when().options("/blogs/1")
                .then()
                .header("Access-Control-Allow-Origin", nullValue());
    }

    @Test
    void simpleRequestFromForeignOrigin_getsNoCorsHeaders() {
        given()
                .header("Origin", FOREIGN_ORIGIN)
                .when().get("/blogs")
                .then()
                .header("Access-Control-Allow-Origin", nullValue());
    }
}
