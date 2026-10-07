package com.ape.friction.api.handlers;

import jakarta.annotation.Nullable;
import jakarta.mail.SendFailedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.javers.common.exception.JaversException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.mail.MailSendException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
//import com.ape.friction.api.exceptions.survey.AiEvaluationFailedException;
import com.ape.friction.api.mok.services.ProblemDetailService;

import static com.ape.friction.api.mok.services.ProblemDetailService.TAG_PREFIX;


@RestControllerAdvice
@RequiredArgsConstructor
public class GeneralControllerExceptionHandler extends ResponseEntityExceptionHandler {

    private final ProblemDetailService problemDetailService;

    private static final Logger log = LoggerFactory.getLogger(GeneralControllerExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {

        return ResponseEntity.status(statusCode).body(problemDetailService.createProblemDetail((HttpStatus) statusCode, ex,null, request));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> onConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        ProblemDetail pd = problemDetailService.createProblemDetail(
                HttpStatus.BAD_REQUEST,
                ex, null, request
        );
        String violations = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .reduce((a, b) -> a + "; " + b).orElse("");
        pd.setProperty("violations", violations);
        return ResponseEntity.badRequest().body(pd);
    }


    // requires security starter
    // For method level authorization exceptions handling
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAuthorizationDenied(
            AuthorizationDeniedException ex, HttpServletRequest request) {

        ProblemDetail pd = problemDetailService.createProblemDetail(
                HttpStatus.FORBIDDEN,ex, null, request
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
    }

    @ExceptionHandler(SendFailedException.class)
    public ResponseEntity<ProblemDetail> handleSendFailed(
            SendFailedException ex, HttpServletRequest request) {

        ProblemDetail pd = problemDetailService.createProblemDetail(
                HttpStatus.BAD_REQUEST, ex, null, request
        );
        return ResponseEntity.badRequest().body(pd);
    }

    // Thrown when Google token exchange HTTP request fails (network issues, invalid code, etc.)
    @ExceptionHandler(java.io.IOException.class)
    public ResponseEntity<ProblemDetail> handleIO(
            java.io.IOException ex, HttpServletRequest request) {

        ProblemDetail pd = problemDetailService.createProblemDetail(
                HttpStatus.BAD_GATEWAY, ex, null, request
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(pd);
    }

    @ExceptionHandler(java.security.GeneralSecurityException.class)
    public ResponseEntity<ProblemDetail> handleGeneralSecurity(
            java.security.GeneralSecurityException ex, HttpServletRequest request) {

        ProblemDetail pd = problemDetailService.createProblemDetail(
                HttpStatus.UNAUTHORIZED, ex, null, request
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }

    @ExceptionHandler(MailSendException.class)
    public ResponseEntity<ProblemDetail> handleMailSend(
            MailSendException ex, HttpServletRequest request) {

        Throwable root = ex;
        while (root.getCause() != null) root = root.getCause();

        boolean invalidAddress = root.getMessage() != null &&
                (root.getMessage().contains("Invalid Addresses") ||
                        root.getMessage().contains("Recipient address rejected"));

        HttpStatus status = invalidAddress ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;

        String code = invalidAddress ? "error.mail-invalid-address" : "error.mail-send-failed";

        ProblemDetail pd = problemDetailService.createProblemDetail(
                status, TAG_PREFIX + ex.getClass().getSimpleName().toLowerCase(), code, null, request
        );
        return ResponseEntity.status(status).body(pd);
    }

    // do poprawy po zrobieniu rozszerzen PROBDETAILS i TRANSL
//    @ExceptionHandler(AiEvaluationFailedException.class)
//    public ResponseEntity<ProblemDetail> handleAiEvaluationFailed(
//            AiEvaluationFailedException ex, HttpServletRequest request) {
//
//        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
//        ProblemDetail pd = problemDetailService.createProblemDetail(
//                status, ex, null, request
//        );
//        return ResponseEntity.status(status).body(pd);
//    }

    @ExceptionHandler(JaversException.class)
    public ResponseEntity<ProblemDetail> handleJaversException(JaversException ex, HttpServletRequest request) {
        ProblemDetail pd = problemDetailService.createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, ex, null, request
        );
        log.error("JaversException stacktrace:", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }

    // do poprawy po zrobieniu rozszerzen PROBDETAILS i TRANSL
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ProblemDetail> handleRestClient(
            RestClientException ex, HttpServletRequest request) {

        ProblemDetail pd = problemDetailService.createProblemDetail(
                HttpStatus.GATEWAY_TIMEOUT,ex, null, request
        );
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(pd);
    }

    @ExceptionHandler(com.stripe.exception.StripeException.class)
    public ProblemDetail handleStripeException(com.stripe.exception.StripeException ex, HttpServletRequest request) {
        ProblemDetail pd = problemDetailService.createProblemDetail(
                HttpStatus.BAD_GATEWAY,ex, null, request
        );

        return pd;
    }



    // For general unknown exceptions handling
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ProblemDetail> handleUncaught(
            RuntimeException ex, HttpServletRequest request) {

        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        ProblemDetail pd = problemDetailService.createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,ex, null, request
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }

}
