package vn.huy.digital_wallet.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 1. Xử lý VALIDATION (400)
    @ExceptionHandler({MethodArgumentNotValidException.class,
            ConstraintViolationException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> handleValidationException(Exception e, WebRequest request) {
        Map<String, List<String>> errors = new HashMap<>();
        // Tối ưu: Lấy message chính xác từ annotation (@NotNull, @Size ...)
        if (e instanceof MethodArgumentNotValidException ex) {
            ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                    errors.computeIfAbsent(fieldError.getField(), k -> new ArrayList<>())
                            .add(fieldError.getDefaultMessage())
            );
        } else if (e instanceof ConstraintViolationException ex) {
            ex.getConstraintViolations().forEach(violation ->
                    errors.computeIfAbsent(violation.getPropertyPath().toString(), k -> new ArrayList<>())
                            .add(violation.getMessage())
            );
        } else if (e instanceof MissingServletRequestParameterException ex) {
            errors.computeIfAbsent(ex.getParameterName(), k -> new ArrayList<>())
                    .add("Tham số bắt buộc bị thiếu");
        }


        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(BAD_REQUEST.value())
                .error(BAD_REQUEST.getReasonPhrase())
                .path(request.getDescription(false).replace("uri=", ""))
                .validationErrors(errors)
                .build();
        return ResponseEntity.status(BAD_REQUEST).body(errorResponse);
    }

    // --- 2. Xử lý AUTHENTICATION (401) ---
    @ExceptionHandler({InternalAuthenticationServiceException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleAuthenticationException(Exception e, WebRequest request) {
        // 1. ghi log cho dev xem
        if (e instanceof InternalAuthenticationServiceException) {
            log.error("Lỗi hệ thống khi login: ", e);
        }
        else log.warn("Đăng nhập thất bại: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(UNAUTHORIZED.value())
                .error(UNAUTHORIZED.getReasonPhrase())
                .path(request.getDescription(false).replace("uri=", ""))
                .message("Username or password is incorrect")
                .build();
        return ResponseEntity.status(UNAUTHORIZED).body(errorResponse);
    }

    // 3. Xử lý quyền truy cập (403)
    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(Exception e, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(FORBIDDEN.value())
                .error(FORBIDDEN.getReasonPhrase())
                .path(request.getDescription(false).replace("uri=", ""))
                .message("Bạn không có quyền truy cập vào tài nguyên này")
                .build();
        return ResponseEntity.status(FORBIDDEN).body(errorResponse);
    }

    // 4. Xử lý không tìm thấy (404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(Exception e, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(NOT_FOUND.value())
                .error(NOT_FOUND.getReasonPhrase())
                .path(request.getDescription(false).replace("uri=", ""))
                .message(e.getMessage())
                .build();
        return ResponseEntity.status(NOT_FOUND).body(errorResponse);
    }

    // --- 5. XỬ LÝ TRÙNG LẶP / CONFLICT (409) ---
    @ExceptionHandler({DuplicateResourceException.class, DuplicateRequestException.class})
    public ResponseEntity<ErrorResponse> handleConflictException(Exception e, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(CONFLICT.value())
                .error(CONFLICT.getReasonPhrase())
                .path(request.getDescription(false).replace("uri=", ""))
                .message(e.getMessage())
                .build();
        return ResponseEntity.status(CONFLICT).body(errorResponse);
    }

    // XỬ LÝ LỖI SAI DỮ LIỆU (400 BAD REQUEST)
    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDataException(InvalidDataException e, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(BAD_REQUEST.value())
                .error(BAD_REQUEST.getReasonPhrase())
                .path(request.getDescription(false).replace("uri=", ""))
                .message(e.getMessage())
                .build();
        return ResponseEntity.status(BAD_REQUEST).body(errorResponse);
    }

    // Xử lý lỗi locked
    @ExceptionHandler(WalletLockedException.class)
    public ResponseEntity<ErrorResponse> handleWalletLockedException(WalletLockedException e, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(LOCKED.value())
                .error(LOCKED.getReasonPhrase())
                .path(request.getDescription(false).replace("uri=", ""))
                .message(e.getMessage())
                .build();
        return ResponseEntity.status(LOCKED).body(errorResponse);
    }

    //  Xử lý lỗi hệ thống (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleInternalServerException(Exception e, WebRequest request) {
        log.error("Internal error at {}: ", request.getDescription(false), e);
        // KHÔNG trả e.getMessage() cho client vì lý do bảo mật
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(INTERNAL_SERVER_ERROR.value())
                .error(INTERNAL_SERVER_ERROR.getReasonPhrase())
                .path(request.getDescription(false).replace("uri=", ""))
                .message("Lỗi hệ thống nội bộ, vui lòng thử lại sau!")
                .build();
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}