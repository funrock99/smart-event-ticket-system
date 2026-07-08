package com.example.smartmaintenance.service;

import com.example.smartmaintenance.dto.response.EventIngestionResponse;
import com.example.smartmaintenance.exception.InvalidIdempotencyRequestException;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

    private final CacheService cacheService;

    public IdempotencyService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public Optional<EventIngestionResponse> getProcessedResult(String idempotencyKey, String requestHash) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }

        Optional<CacheService.CachedIdempotencyRecord> record = cacheService.getIdempotencyRecord(idempotencyKey);
        if (record.isEmpty()) {
            return Optional.empty();
        }

        if (!record.get().requestHash().equals(requestHash)) {
            throw new InvalidIdempotencyRequestException(
                    "Idempotency-Key was already used with a different request payload"
            );
        }

        return Optional.of(record.get().response());
    }

    public void saveProcessedResult(String idempotencyKey, String requestHash, EventIngestionResponse response) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        cacheService.cacheIdempotencyRecord(idempotencyKey, requestHash, response);
    }
}