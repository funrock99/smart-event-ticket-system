package com.example.smarteventticket.service;

import com.example.smarteventticket.dto.response.AlarmEventResponse;
import com.example.smarteventticket.dto.response.DashboardSummaryResponse;
import com.example.smarteventticket.dto.response.EventIngestionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);
    private static final DateTimeFormatter RATE_LIMIT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private static final String DASHBOARD_SUMMARY_KEY = "dashboard:summary";
    private static final String EQUIPMENT_STATUS_KEY_PREFIX = "equipment:status:";
    private static final String RECENT_ALARM_KEY = "alarm:recent";
    private static final String ALARM_DEDUP_KEY_PREFIX = "alarm:dedup:";
    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";
    private static final String RATE_LIMIT_KEY_PREFIX = "rate:";
    private static final String DUPLICATE_EVENT_COUNT_KEY = "metrics:duplicate-events";
    private static final String RATE_LIMITED_EVENT_COUNT_KEY = "metrics:rate-limited-events";
    private static final String IDEMPOTENT_REPLAY_COUNT_KEY = "metrics:idempotent-replayed-events";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration dashboardSummaryTtl;
    private final Duration dashboardSummaryEvictDebounce;
    private final Duration equipmentStatusTtl;
    private final Duration recentAlarmTtl;
    private final Duration alarmDedupTtl;
    private final Duration idempotencyProcessingTtl;
    private final Duration idempotencyCompletedTtl;
    private final Duration idempotencyFailedTtl;
    private final Duration rateLimitWindow;
    private final long recentAlarmLimit;
    private final AtomicLong lastDashboardSummaryEvictionAt = new AtomicLong(0L);

    public CacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.redis.dashboard-summary-ttl:10m}") Duration dashboardSummaryTtl,
            @Value("${app.redis.dashboard-summary-evict-debounce:500ms}") Duration dashboardSummaryEvictDebounce,
            @Value("${app.redis.equipment-status-ttl:30m}") Duration equipmentStatusTtl,
            @Value("${app.redis.recent-alarm-ttl:30m}") Duration recentAlarmTtl,
            @Value("${app.redis.alarm-dedup-ttl:2m}") Duration alarmDedupTtl,
            @Value("${app.redis.idempotency-processing-ttl:10m}") Duration idempotencyProcessingTtl,
            @Value("${app.redis.idempotency-completed-ttl:24h}") Duration idempotencyCompletedTtl,
            @Value("${app.redis.idempotency-failed-ttl:10m}") Duration idempotencyFailedTtl,
            @Value("${app.redis.rate-limit-window:1m}") Duration rateLimitWindow,
            @Value("${app.redis.recent-alarm-limit:20}") long recentAlarmLimit
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.dashboardSummaryTtl = dashboardSummaryTtl;
        this.dashboardSummaryEvictDebounce = dashboardSummaryEvictDebounce;
        this.equipmentStatusTtl = equipmentStatusTtl;
        this.recentAlarmTtl = recentAlarmTtl;
        this.alarmDedupTtl = alarmDedupTtl;
        this.idempotencyProcessingTtl = idempotencyProcessingTtl;
        this.idempotencyCompletedTtl = idempotencyCompletedTtl;
        this.idempotencyFailedTtl = idempotencyFailedTtl;
        this.rateLimitWindow = rateLimitWindow;
        this.recentAlarmLimit = recentAlarmLimit;
    }

    public Optional<DashboardSummaryResponse> getDashboardSummary() {
        return readValue(DASHBOARD_SUMMARY_KEY, DashboardSummaryResponse.class);
    }

    public void cacheDashboardSummary(DashboardSummaryResponse response) {
        writeValue(DASHBOARD_SUMMARY_KEY, response, dashboardSummaryTtl);
    }

    public void evictDashboardSummary() {
        deleteKey(DASHBOARD_SUMMARY_KEY);
    }

    public void evictDashboardSummaryThrottled() {
        long now = System.currentTimeMillis();
        long debounceMillis = Math.max(0L, dashboardSummaryEvictDebounce.toMillis());
        long previous = lastDashboardSummaryEvictionAt.get();
        if (now - previous < debounceMillis) {
            return;
        }
        if (lastDashboardSummaryEvictionAt.compareAndSet(previous, now)) {
            deleteKey(DASHBOARD_SUMMARY_KEY);
        }
    }

    public void cacheEquipmentStatus(String equipmentId, Enum<?> status) {
        writeString(EQUIPMENT_STATUS_KEY_PREFIX + equipmentId, status.name(), equipmentStatusTtl);
    }

    public void cacheRecentAlarm(AlarmEventResponse response) {
        try {
            String payload = objectMapper.writeValueAsString(response);
            redisTemplate.opsForList().leftPush(RECENT_ALARM_KEY, payload);
            redisTemplate.opsForList().trim(RECENT_ALARM_KEY, 0, recentAlarmLimit - 1);
            redisTemplate.expire(RECENT_ALARM_KEY, recentAlarmTtl);
        } catch (Exception ex) {
            log.warn("Unable to cache recent alarm in Redis: {}", ex.getMessage());
        }
    }

    public List<AlarmEventResponse> getRecentAlarms() {
        try {
            List<String> payloads = redisTemplate.opsForList().range(RECENT_ALARM_KEY, 0, recentAlarmLimit - 1);
            if (payloads == null || payloads.isEmpty()) {
                return List.of();
            }
            return payloads.stream()
                    .map(this::deserializeAlarm)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
        } catch (Exception ex) {
            log.warn("Unable to read recent alarms from Redis: {}", ex.getMessage());
            return List.of();
        }
    }

    public boolean acquireAlarmDedupKey(String source, String eventType, String businessKey) {
        return writeIfAbsent(
                buildDedupKey(source, eventType, businessKey),
                new CachedDedupRecord(null),
                alarmDedupTtl
        );
    }

    public void updateAlarmDedupTicketId(String source, String eventType, String businessKey, Long ticketId) {
        writeValue(buildDedupKey(source, eventType, businessKey), new CachedDedupRecord(ticketId), alarmDedupTtl);
    }

    public Optional<Long> getAlarmDedupTicketId(String source, String eventType, String businessKey) {
        return readValue(buildDedupKey(source, eventType, businessKey), CachedDedupRecord.class)
                .map(CachedDedupRecord::ticketId)
                .filter(ticketId -> ticketId != null);
    }

    public Optional<CachedIdempotencyRecord> getIdempotencyRecord(String idempotencyKey) {
        return readValue(IDEMPOTENCY_KEY_PREFIX + idempotencyKey, CachedIdempotencyRecord.class);
    }

    public boolean acquireIdempotencyProcessing(String idempotencyKey, String requestHash) {
        return writeIfAbsent(
                IDEMPOTENCY_KEY_PREFIX + idempotencyKey,
                new CachedIdempotencyRecord("PROCESSING", requestHash, null, null),
                idempotencyProcessingTtl
        );
    }

    public void markIdempotencyCompleted(String idempotencyKey, String requestHash, EventIngestionResponse response) {
        writeValue(
                IDEMPOTENCY_KEY_PREFIX + idempotencyKey,
                new CachedIdempotencyRecord("COMPLETED", requestHash, response, null),
                idempotencyCompletedTtl
        );
    }

    public void markIdempotencyFailed(String idempotencyKey, String requestHash, String errorCode) {
        writeValue(
                IDEMPOTENCY_KEY_PREFIX + idempotencyKey,
                new CachedIdempotencyRecord("FAILED", requestHash, null, errorCode),
                idempotencyFailedTtl
        );
    }

    public long incrementRateRequestCount(String source) {
        String key = buildRateLimitKey(source);
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, rateLimitWindow);
            }
            return count == null ? 0L : count;
        } catch (Exception ex) {
            log.warn("Unable to increment rate limit counter in Redis: {}", ex.getMessage());
            return 0L;
        }
    }

    public void incrementDuplicateEventCount() {
        incrementMetric(DUPLICATE_EVENT_COUNT_KEY);
    }

    public long getDuplicateEventCount() {
        return getMetric(DUPLICATE_EVENT_COUNT_KEY);
    }

    public void incrementRateLimitedEventCount() {
        incrementMetric(RATE_LIMITED_EVENT_COUNT_KEY);
    }

    public long getRateLimitedEventCount() {
        return getMetric(RATE_LIMITED_EVENT_COUNT_KEY);
    }

    public void incrementIdempotentReplayCount() {
        incrementMetric(IDEMPOTENT_REPLAY_COUNT_KEY);
    }

    public long getIdempotentReplayCount() {
        return getMetric(IDEMPOTENT_REPLAY_COUNT_KEY);
    }

    public String buildDedupKey(String source, String eventType, String businessKey) {
        return ALARM_DEDUP_KEY_PREFIX + source + ":" + eventType + ":" + businessKey;
    }

    public String buildRateLimitKey(String source) {
        return RATE_LIMIT_KEY_PREFIX + source + ":" + LocalDateTime.now().format(RATE_LIMIT_FORMATTER);
    }

    private void incrementMetric(String key) {
        try {
            redisTemplate.opsForValue().increment(key);
        } catch (Exception ex) {
            log.warn("Unable to increment Redis metric {}: {}", key, ex.getMessage());
        }
    }

    private long getMetric(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value == null ? 0L : Long.parseLong(value);
        } catch (Exception ex) {
            log.warn("Unable to read Redis metric {}: {}", key, ex.getMessage());
            return 0L;
        }
    }

    private Optional<AlarmEventResponse> deserializeAlarm(String payload) {
        try {
            return Optional.of(objectMapper.readValue(payload, AlarmEventResponse.class));
        } catch (Exception ex) {
            log.warn("Unable to deserialize cached alarm: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private <T> Optional<T> readValue(String key, Class<T> type) {
        try {
            String payload = redisTemplate.opsForValue().get(key);
            if (payload == null || payload.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(payload, type));
        } catch (Exception ex) {
            log.warn("Unable to read Redis key {}: {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    private void writeValue(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception ex) {
            log.warn("Unable to write Redis key {}: {}", key, ex.getMessage());
        }
    }

    private void writeString(String key, String value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception ex) {
            log.warn("Unable to write Redis key {}: {}", key, ex.getMessage());
        }
    }

    private boolean writeIfAbsent(String key, Object value, Duration ttl) {
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, objectMapper.writeValueAsString(value), ttl);
            return Boolean.TRUE.equals(acquired);
        } catch (Exception ex) {
            log.warn("Unable to acquire Redis key {}: {}", key, ex.getMessage());
            return true;
        }
    }

    private void deleteKey(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception ex) {
            log.warn("Unable to delete Redis key {}: {}", key, ex.getMessage());
        }
    }

    public record CachedIdempotencyRecord(
            String status,
            String requestHash,
            EventIngestionResponse response,
            String errorCode
    ) {
    }

    public record CachedDedupRecord(Long ticketId) {
    }
}
