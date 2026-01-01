package com.alphaskyport.admin.exception;

import org.springframework.http.HttpStatus;

public class AdminException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public AdminException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    // Authentication Exceptions
    public static class InvalidCredentialsException extends AdminException {
        public InvalidCredentialsException(String message) {
            super(message, HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }
    }

    public static class InvalidTokenException extends AdminException {
        public InvalidTokenException(String message) {
            super(message, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN");
        }
    }

    public static class AccountLockedException extends AdminException {
        public AccountLockedException(String message) {
            super(message, HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED");
        }
    }

    public static class AccountDisabledException extends AdminException {
        public AccountDisabledException(String message) {
            super(message, HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED");
        }
    }

    // Authorization Exceptions
    public static class InsufficientPermissionsException extends AdminException {
        public InsufficientPermissionsException(String message) {
            super(message, HttpStatus.FORBIDDEN, "INSUFFICIENT_PERMISSIONS");
        }
    }

    // Not Found Exceptions
    public static class NotFoundException extends AdminException {
        public NotFoundException(String message) {
            super(message, HttpStatus.NOT_FOUND, "NOT_FOUND");
        }
    }

    public static class AdminNotFoundException extends AdminException {
        public AdminNotFoundException(String message) {
            super(message, HttpStatus.NOT_FOUND, "ADMIN_NOT_FOUND");
        }
    }

    // Validation/State Exceptions
    public static class DuplicateEmailException extends AdminException {
        public DuplicateEmailException(String message) {
            super(message, HttpStatus.CONFLICT, "DUPLICATE_EMAIL");
        }
    }

    public static class InvalidStateException extends AdminException {
        public InvalidStateException(String message) {
            super(message, HttpStatus.BAD_REQUEST, "INVALID_STATE");
        }
    }

    public static class OperationNotAllowedException extends AdminException {
        public OperationNotAllowedException(String message) {
            super(message, HttpStatus.BAD_REQUEST, "OPERATION_NOT_ALLOWED");
        }
    }

    public static class ValidationException extends AdminException {
        public ValidationException(String message) {
            super(message, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
        }
    }

    // Concurrency Exception
    public static class OptimisticLockException extends AdminException {
        public OptimisticLockException(String message) {
            super(message, HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION");
        }
    }
}
