package com.homekm.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class IdempotencyPurgeJob {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyPurgeJob.class);

    private final IdempotencyKeyRepository repository;

    public IdempotencyPurgeJob(IdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    @Scheduled(fixedDelay = 6 * 60 * 60_000L, initialDelay = 60_000L)
    @Transactional
    public void purge() {
        int deleted = repository.purgeExpired(Instant.now());
        if (deleted > 0) log.info("idempotency: purged {} expired records", deleted);
    }
}
