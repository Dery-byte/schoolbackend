package com.alibou.book.handler;

import com.alibou.book.Errors.ErrorResponse;
import com.alibou.book.exception.*;
import jakarta.mail.MessagingException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.alibou.book.handler.BusinessErrorCodes.ACCOUNT_DISABLED;
import static com.alibou.book.handler.BusinessErrorCodes.ACCOUNT_LOCKED;
import static com.alibou.book.handler.BusinessErrorCodes.BAD_CREDENTIALS;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ExceptionResponse> handleException(LockedException exp) {
        return ResponseEntity
                .status(UNAUTHORIZED)
                .body(
                        ExceptionResponse.builder()
                                .businessErrorCode(ACCOUNT_LOCKED.getCode())
                                .businessErrorDescription(ACCOUNT_LOCKED.getDescription())
                                .error(exp.getMessage())
                                .build()
                );
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ExceptionResponse> handleException(DisabledException exp) {
        return ResponseEntity
                .status(UNAUTHORIZED)
                .body(
                        ExceptionResponse.builder()
                                .businessErrorCode(ACCOUNT_DISABLED.getCode())
                                .businessErrorDescription(ACCOUNT_DISABLED.getDescription())
                                .error(exp.getMessage())
                                .build()
                );
    }


    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ExceptionResponse> handleException() {
        return ResponseEntity
                .status(UNAUTHORIZED)
                .body(
                        ExceptionResponse.builder()
                                .businessErrorCode(BAD_CREDENTIALS.getCode())
                                .businessErrorDescription(BAD_CREDENTIALS.getDescription())
                                .error(BAD_CREDENTIALS.getDescription())
                                .build()
                );
    }

    @ExceptionHandler(MessagingException.class)
    public ResponseEntity<ExceptionResponse> handleException(MessagingException exp) {
        return ResponseEntity
                .status(INTERNAL_SERVER_ERROR)
                .body(
                        ExceptionResponse.builder()
                                .error(exp.getMessage())
                                .build()
                );
    }

    @ExceptionHandler(ActivationTokenException.class)
    public ResponseEntity<ExceptionResponse> handleException(ActivationTokenException exp) {
        return ResponseEntity
                .status(BAD_REQUEST)
                .body(
                        ExceptionResponse.builder()
                                .error(exp.getMessage())
                                .build()
                );
    }

    @ExceptionHandler(OperationNotPermittedException.class)
    public ResponseEntity<ExceptionResponse> handleException(OperationNotPermittedException exp) {
        return ResponseEntity
                .status(BAD_REQUEST)
                .body(
                        ExceptionResponse.builder()
                                .error(exp.getMessage())
                                .build()
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException exp) {
        Set<String> errors = new HashSet<>();
        exp.getBindingResult().getAllErrors()
                .forEach(error -> {
                    var fieldName = ((FieldError) error).getField();
                    var errorMessage = error.getDefaultMessage();
                    errors.add(errorMessage);
                });

        return ResponseEntity
                .status(BAD_REQUEST)
                .body(
                        ExceptionResponse.builder()
                                .validationErrors(errors)
                                .build()
                );
    }

//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ExceptionResponse> handleException(Exception exp) {
//        exp.printStackTrace();
//        return ResponseEntity
//                .status(INTERNAL_SERVER_ERROR)
//                .body(
//                        ExceptionResponse.builder()
//                                .businessErrorDescription("Internal error, please contact the admin")
//                                .error(exp.getMessage())
//                                .build()
//                );
//    }






//    @ExceptionHandler(DuplicateEmailException.class)
//    public ResponseEntity<ExceptionResponse> handleDuplicateEmail(DuplicateEmailException ex) {
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .body(ExceptionResponse.builder()
//                        .businessErrorDescription(ex.getMessage())
//                        .error("Email conflict")
//                        .build());
//    }

    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<ExceptionResponse> handleDuplicateUsername(DuplicateUsernameException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ExceptionResponse.builder()
                        .businessErrorDescription(ex.getMessage())
                        .error("Username conflict")
                        .build());
    }

















//
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, WebRequest request) {
//        ex.printStackTrace();
//
//        String errorMessage = ex.getMessage();
//        String message = "An unexpected error occurred";
//
//        // Check for duplicate key errors from DB
//        if (errorMessage != null && errorMessage.contains("Cannot insert duplicate key")) {
//            if (errorMessage.contains("UK_nlcolwbx8ujaen5h0u2kr2bn2")) { // your email constraint name
//                message = "An account with this email already exists.";
//            } else if (errorMessage.contains("UK_username")) { // your username constraint name
//                message = "Username is already taken.";
//            }
//        }
//
//        ErrorResponse errorResponse = new ErrorResponse(
//                LocalDateTime.now(),
//                HttpStatus.CONFLICT.value(), // 409 is the right code for duplicates
//                "Conflict",
//                message,
//                request.getDescription(false).replace("uri=", "")
//        );
//
//        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
//    }




    @ExceptionHandler(InvalidAgeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAge(InvalidAgeException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Invalid Age",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }



    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Object> handleDuplicateEmail(DuplicateEmailException ex) {
//        System.out.println("=== EXCEPTION HANDLER DEBUG ===");
//        System.out.println("Exception class: " + ex.getClass().getName());
//        System.out.println("Exception message: '" + ex.getMessage() + "'");
//        System.out.println("Message is null: " + (ex.getMessage() == null));
//        System.out.println("===============================");

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Duplicate Email");
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }


    // fallback catch-all
}

