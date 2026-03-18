package ec.upec.jasper.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReportTemplateNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTemplateNotFound(
            ReportTemplateNotFoundException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.NOT_FOUND, "Template Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiErrorResponse> handleValidation(
            Exception ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST, "Validation Error", ex.getMessage(), request);
    }

    @ExceptionHandler(ReportGenerationException.class)
    public ResponseEntity<ApiErrorResponse> handleReportGeneration(
            ReportGenerationException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Report Generation Error", buildReportErrorMessage(ex), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", ex.getMessage(), request);
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                error,
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(response);
    }

    private String buildReportErrorMessage(ReportGenerationException ex) {
        Throwable rootCause = ex;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }

        String rootMessage = rootCause.getMessage();
        if (rootMessage == null || rootMessage.isBlank()) {
            return ex.getMessage();
        }

        return ex.getMessage() + " | Causa raíz: " + rootMessage;
    }
}
