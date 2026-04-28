package com.homekm.common;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.Callable;

@Component
public class MinioGateway {

    private static final Logger log = LoggerFactory.getLogger(MinioGateway.class);

    private final Retry retry;
    private final CircuitBreaker breaker;
    private final Bulkhead bulkhead;

    public MinioGateway(RetryRegistry retryRegistry,
                         CircuitBreakerRegistry breakerRegistry,
                         BulkheadRegistry bulkheadRegistry) {
        this.retry = retryRegistry.retry("minio");
        this.breaker = breakerRegistry.circuitBreaker("minio");
        this.bulkhead = bulkheadRegistry.bulkhead("minio");
    }

    @FunctionalInterface
    public interface MinioCall<T> {
        T call() throws Exception;
    }

    public <T> T call(MinioCall<T> op) throws Exception {
        Callable<T> base = op::call;
        Callable<T> guarded = Bulkhead.decorateCallable(bulkhead,
                CircuitBreaker.decorateCallable(breaker,
                        Retry.decorateCallable(retry, base)));
        try {
            return guarded.call();
        } catch (CallNotPermittedException e) {
            log.warn("MinIO circuit open: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "STORAGE_UNAVAILABLE");
        }
    }

    public void run(Runnable op) throws Exception {
        call(() -> { op.run(); return null; });
    }
}
