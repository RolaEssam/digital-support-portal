package com.scbbank.supportportal.auth.exception;

//import com.scbbank.supportportal.auth.dto.base.ApiResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    // 1) كل أخطاء الـ Auth (401 / 423 / 400 ...)
    /*@ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> handleAuth(AuthException ex) {
        ErrorCode ec = ex.getErrorCode();
        Map<String, Object> details = ex.getDetails();

        ApiResponseDto<Map<String, Object>> body = details.isEmpty()
                ? ApiResponseDto.error(ec.getCode(), message(ec, ex.getArgs()))
                : ApiResponseDto.error(ec.getCode(), message(ec, ex.getArgs()), details);

        return ResponseEntity.status(ec.getStatus()).body(body);
    } */


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> handleValidation(
            MethodArgumentNotValidException ex) {

        ErrorCode ec = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> mapField(fe.getField(), fe.getCode()))
                .orElse(ErrorCode.INVALID_LENGTH);

        return ResponseEntity.status(ec.getStatus())
                .body(ApiResponseDto.error(ec.getCode(), message(ec)));
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> handleUnreadable(
            HttpMessageNotReadableException ex) {
        ErrorCode ec = ErrorCode.INVALID_REQUEST_ID;
        return ResponseEntity.status(ec.getStatus())
                .body(ApiResponseDto.error(ec.getCode(), message(ec)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> handleUnexpected(Exception ex) {
        String correlationId = UUID.randomUUID().toString();
        log.error("Unexpected error [correlationId={}]", correlationId, ex);

        ErrorCode ec = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(ec.getStatus())
                .body(ApiResponseDto.error(ec.getCode(), message(ec)));
    }

    // ---------- helpers ----------

    private ErrorCode mapField(String field, String constraint) {
        return switch (field) {
            case "requestId" -> ErrorCode.INVALID_REQUEST_ID;
            case "username"  -> isBlank(constraint) ? ErrorCode.USERNAME_REQUIRED : ErrorCode.INVALID_LENGTH;
            case "password"  -> isBlank(constraint) ? ErrorCode.PASSWORD_REQUIRED : ErrorCode.INVALID_LENGTH;
            default          -> ErrorCode.INVALID_LENGTH;
        };
    }

    private boolean isBlank(String constraint) {
        return "NotBlank".equals(constraint) || "NotNull".equals(constraint) || "NotEmpty".equals(constraint);
    }

    private String message(ErrorCode ec, Object... args) {
        return messageSource.getMessage(ec.getMessageKey(), args, LocaleContextHolder.getLocale());
    }
}