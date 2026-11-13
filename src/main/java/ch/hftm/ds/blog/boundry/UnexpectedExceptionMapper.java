package ch.hftm.ds.blog.boundry;

import io.quarkus.logging.Log;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Fängt unerwartete Fehler ab: Sie werden mit Stacktrace geloggt, der Client
 * erhält nur eine generische 500-Meldung ohne interne Details.
 * HTTP-Fehler, die bewusst geworfen werden (z. B. 404 für unbekannte Pfade),
 * werden unverändert weitergegeben.
 */
@Provider
public class UnexpectedExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof WebApplicationException webException) {
            return webException.getResponse();
        }
        Log.errorf(exception, "Unexpected error: %s", exception.getMessage());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Internal server error"))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
