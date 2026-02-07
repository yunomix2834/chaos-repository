package com.chaos.task_manager.exception;

import com.chaos.task_manager.dto.common.ErrorResponse;
import com.chaos.task_manager.utils.CommonUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Log4j2
@RestControllerAdvice
public class AppExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({AppException.class})
    public Mono<ResponseEntity<ErrorResponse>> handleAppException(
            AppException ex,
            ServerWebExchange request) {
        ErrorResponse errorResponse = ex.getErrorResponse();
        HttpStatus httpStatus = CommonUtils.getHttpStatus(errorResponse.getCode());
        return Mono.just(ResponseEntity.status(httpStatus).body(errorResponse));
    }
}
