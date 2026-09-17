package ch.hftm.ds.blog.boundry;

import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Wandelt fehlgeschlagene Bean-Validation (@Valid) in eine aussagekräftige
 * 400 Bad Request Response mit den konkreten Verletzungen um, statt des
 * generischen Standard-Fehlers.
 */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "Validation failed";
        }

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
