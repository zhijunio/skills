package com.example.sb4jpavueproduct.product;

import java.net.URI;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class ProductExceptionHandler extends ResponseEntityExceptionHandler {

	private static final URI ERROR_BASE = URI.create("https://errors.example.com/products/");

	@ExceptionHandler(ProductNotFoundException.class)
	ProblemDetail notFound(ProductNotFoundException exception) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
		problem.setType(ERROR_BASE.resolve("not-found"));
		problem.setTitle("Product not found");
		problem.setProperty("errorCode", "PRODUCT_NOT_FOUND");
		return problem;
	}

	@ExceptionHandler(ProductSkuConflictException.class)
	ProblemDetail skuConflict(ProductSkuConflictException exception) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problem.setType(ERROR_BASE.resolve("sku-conflict"));
		problem.setTitle("SKU already exists");
		problem.setProperty("errorCode", "PRODUCT_SKU_CONFLICT");
		return problem;
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail dataConflict(DataIntegrityViolationException exception) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"The request conflicts with the current database state.");
		problem.setType(ERROR_BASE.resolve("data-conflict"));
		problem.setTitle("Data conflict");
		problem.setProperty("errorCode", "DATA_INTEGRITY_CONFLICT");
		return problem;
	}

	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(
			HandlerMethodValidationException exception,
			org.springframework.http.HttpHeaders headers,
			org.springframework.http.HttpStatusCode status,
			WebRequest request) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed.");
		problem.setType(ERROR_BASE.resolve("validation"));
		problem.setTitle("Validation failed");
		problem.setProperty("errorCode", "REQUEST_VALIDATION_FAILED");
		return handleExceptionInternal(exception, problem, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException exception,
			org.springframework.http.HttpHeaders headers,
			org.springframework.http.HttpStatusCode status,
			WebRequest request) {
		var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed.");
		problem.setType(ERROR_BASE.resolve("validation"));
		problem.setTitle("Validation failed");
		problem.setProperty("violations", exception.getBindingResult().getFieldErrors().stream()
				.map(error -> Map.of("field", error.getField(), "message",
						error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()))
				.toList());
		return handleExceptionInternal(exception, problem, headers, status, request);
	}
}
