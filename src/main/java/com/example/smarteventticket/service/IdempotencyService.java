package com.example.smarteventticket.service;

import com.example.smarteventticket.dto.response.EventIngestionResponse;
import com.example.smarteventticket.exception.InvalidIdempotencyRequestException;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

    private final CacheService cacheService;

    public IdempotencyService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public IdempotencyStartResult startRequest(String idempotencyKey, String requestHash) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return IdempotencyStartResult.startNew();
        }

        Optional<CacheService.CachedIdempotencyRecord> record = cacheService.getIdempotencyRecord(idempotencyKey);
        if (record.isEmpty()) {
            boolean acquired = cacheService.acquireIdempotencyProcessing(idempotencyKey, requestHash);
            if (acquired) {
                return IdempotencyStartResult.startNew();
            }
            return startRequest(idempotencyKey, requestHash);
        }

        CacheService.CachedIdempotencyRecord cachedRecord = record.get();
        if (!cachedRecord.requestHash().equals(requestHash)) {
            throw new InvalidIdempotencyRequestException(
                    "Same Idempotency-Key was used with different request body."
            );
        }

        return switch (cachedRecord.status()) {
            case "COMPLETED" -> {
                cacheService.incrementIdempotentReplayCount();
                yield IdempotencyStartResult.replay(cachedRecord.response());
            }
            case "PROCESSING" -> IdempotencyStartResult.inProgress();
            case "FAILED" -> IdempotencyStartResult.failedState();
            default -> IdempotencyStartResult.startNew();
        };
    }

    public void markCompleted(String idempotencyKey, String requestHash, EventIngestionResponse response) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        cacheService.markIdempotencyCompleted(idempotencyKey, requestHash, response);
    }

    public void markFailed(String idempotencyKey, String requestHash, String errorCode) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        cacheService.markIdempotencyFailed(idempotencyKey, requestHash, errorCode);
    }

    public record IdempotencyStartResult(
            boolean started,
            boolean processing,
            boolean failed,
            Optional<EventIngestionResponse> replayedResponse
    ) {
        public static IdempotencyStartResult startNew() {
            return new IdempotencyStartResult(true, false, false, Optional.empty());
        }

        public static IdempotencyStartResult inProgress() {
            return new IdempotencyStartResult(false, true, false, Optional.empty());
        }

        public static IdempotencyStartResult failedState() {
            return new IdempotencyStartResult(false, false, true, Optional.empty());
        }

        public static IdempotencyStartResult replay(EventIngestionResponse response) {
            return new IdempotencyStartResult(false, false, false, Optional.of(response));
        }
    }
}
