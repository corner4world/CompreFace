/*
 * Copyright (c) 2020 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.exadel.frs.commonservice.handler;

import com.exadel.frs.commonservice.dto.ExceptionResponseDto;
import com.exadel.frs.commonservice.exception.BasicException;
import com.exadel.frs.commonservice.exception.ConstraintViolationException;
import com.exadel.frs.commonservice.exception.DemoNotAvailableException;
import com.exadel.frs.commonservice.exception.EmptyRequiredFieldException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static com.exadel.frs.commonservice.handler.CommonExceptionCode.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ControllerAdvice
@Slf4j
public class ResponseExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BasicException.class)
    public ResponseEntity<ExceptionResponseDto> handleDefinedExceptions(final BasicException ex) {
        log.error("Defined exception occurred", ex);

        return ResponseEntity
                .status(ex.getExceptionCode().getHttpStatus())
                .body(buildBody(ex));
    }


    @ExceptionHandler(value = {MissingRequestHeaderException.class})
    public ResponseEntity<ExceptionResponseDto> handleMissingRequestHeader(final MissingRequestHeaderException e) {
        return handleMissingRequestHeader(e.getHeaderName());
    }

    public ResponseEntity<ExceptionResponseDto> handleMissingRequestHeader(final String headerName) {
        log.error("Missing header exception: " + headerName);

        return ResponseEntity.status(MISSING_REQUEST_HEADER.getHttpStatus())
                .body(buildBody(MISSING_REQUEST_HEADER.getCode(), "Missing header: " + headerName));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        log.error("Field validation failed", ex);

        FieldError fieldError = ex.getBindingResult().getFieldErrors().get(0);
        BasicException basicException = getException(fieldError);

        return ResponseEntity
                .status(status)
                .body(buildBody(basicException));
    }

    @ExceptionHandler(javax.validation.ConstraintViolationException.class)
    ResponseEntity<ExceptionResponseDto> handleConstraintViolationException(javax.validation.ConstraintViolationException e) {
        log.error("Constraint violation exception occurred", e);

        val sb = new StringBuilder();
        for (val violation : e.getConstraintViolations()) {
            sb.append(e.getMessage());
            sb.append("; ");
        }

        return ResponseEntity.status(VALIDATION_CONSTRAINT_VIOLATION.getHttpStatus())
                .body(buildBody(VALIDATION_CONSTRAINT_VIOLATION.getCode(), sb.toString()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponseDto> handleUndefinedExceptions(final Exception ex) {
        log.error("Undefined exception occurred", ex);

        return ResponseEntity.status(UNDEFINED.getHttpStatus()).body(buildBody());
    }

    @ExceptionHandler(DemoNotAvailableException.class)
    public ResponseEntity<ExceptionResponseDto> handleDemoNotAvailableException(final DemoNotAvailableException ex) {
        return ResponseEntity.status(NOT_FOUND).body(ExceptionResponseDto.builder().code(NOT_FOUND.value()).message(DemoNotAvailableException.MESSAGE).build());
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        log.error("404 error has occurred", ex);

        val body = ExceptionResponseDto.builder()
                .code(UNDEFINED.getCode())
                .message(ex.getMessage() != null ? ex.getMessage() : "No message available")
                .build();

        return ResponseEntity.status(NOT_FOUND).body(body);
    }

    private BasicException getException(final FieldError fieldError) {
        BasicException basicException;

        switch (fieldError.getCode()) {
            case "NotBlank":
            case "ValidEnum":
                basicException = new ConstraintViolationException(fieldError.getDefaultMessage());
                break;
            case "NotNull":
            case "NotEmpty":
                basicException = new EmptyRequiredFieldException(fieldError.getField());
                break;
            case "Size":
                basicException = new ConstraintViolationException(fieldError.getField(), fieldError.getDefaultMessage());
                break;
            default:
                basicException = new BasicException(UNDEFINED, "");
        }

        return basicException;
    }

    private ExceptionResponseDto buildBody(final BasicException ex) {
        return ExceptionResponseDto.builder()
                .code(ex.getExceptionCode().getCode())
                .message(ex.getMessage())
                .build();
    }

    private ExceptionResponseDto buildBody(Integer code, String message) {
        return ExceptionResponseDto.builder()
                .code(code)
                .message(message)
                .build();
    }

    private ExceptionResponseDto buildBody() {
        return ExceptionResponseDto.builder()
                .code(UNDEFINED.getCode())
                .message("Something went wrong, please try again")
                .build();
    }

}