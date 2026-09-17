package ch.hftm.ds.blog.boundry;

/**
 * Einheitliches JSON-Format für Fehler-Responses:
 * { "message": "..." }
 */
public class ErrorResponse {

    private String message;

    public ErrorResponse() {
    }

    public ErrorResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
