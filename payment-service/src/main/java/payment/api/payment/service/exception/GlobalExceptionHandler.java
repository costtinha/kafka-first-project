package payment.api.payment.service.exception;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(PaymentNotFoundException.class)
    public ProblemDetail handleNoFound(PaymentNotFoundException ex,
                                       HttpServletRequest request){
        log.warn("Resource not found : {}",ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,ex.getMessage());
        problem.setTitle("Resource not found");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setType(URI.create("errors/not-found"));
        problem.setProperty("timestamp", LocalDateTime.now());
        return problem;

    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request){
        log.warn("Illegal argument at payment: {}",ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Bad request");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setType(URI.create("errors/bad-request"));
        problem.setProperty("timestamp",LocalDateTime.now());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception ex, HttpServletRequest request){
        log.error("Unexpected error on {}: {}",request.getRequestURI(),ex.getMessage(),ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());

        problem.setTitle("Internal server error");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setType(URI.create("errors/internal-error"));
        problem.setProperty("timestamp",LocalDateTime.now());
        return problem;
    }

}
