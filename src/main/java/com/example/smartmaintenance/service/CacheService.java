package com.example.smartmaintenance.service;

import com.example.smartmaintenance.dto.response.AlarmEventResponse;
import com.example.smartmaintenance.dto.response.DashboardSummaryResponse;
import com.example.smartmaintenance.enums.EquipmentStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    private static final String DASHBOARD_SUMMARY_KEY = "dashboard:summary";
    private static final String EQUIPMENT_STATUS_KEY_PREFIX = "equipment:status:";
    private static final String RECENT_ALARM_KEY = "alarm:recent";
    private static final String ALARM_DEDUP_KEY_PREFIX = "alarm:dedup:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration dashboardSummaryTtl;
    private final Duration equipmentStatusTtl;
    private final Duration recentAlarmTtl;
    private final Duration alarmDedupTtl;
    private final long recentAlarmLimit;

    public CacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.redis.dashboard-summary-ttl:10m}") Duration dashboardSummaryTtl,
            @Value("${app.redis.equipment-status-ttl:30m}") Duration equipmentStatusTtl,
            @Value("${app.redis.recent-alarm-ttl:30m}") Duration recentAlarmTtl,
            @Value("${app.redis.alarm-dedup-ttl:2m}") Duration alarmDedupTtl,
            @Value("${app.redis.recent-alarm-limit:20}") long recentAlarmLimit
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.dashboardSummaryTtl = dashboardSummaryTtl;
        this.equipmentStatusTtl = equipmentStatusTtl;
        this.recentAlarmTtl = recentAlarmTtl;
        this.alarmDedupTtl = alarmDedupTtl;
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

    public void cacheEquipmentStatus(String equipmentId, EquipmentStatus status) {
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
                    .map(payload -> deserializeAlarm(payload))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
        } catch (Exception ex) {
            log.warn("Unable to read recent alarms from Redis: {}", ex.getMessage());
            return List.of();
        }
    }

    public boolean acquireAlarmDedupKey(String equipmentId, String alarmCode) {
        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(ALARM_DEDUP_KEY_PREFIX + equipmentId + ":" + alarmCode, "1", alarmDedupTtl);
            return Boolean.TRUE.equals(acquired);
        } catch (Exception ex) {
            log.warn("Unable to acquire alarm dedup key from Redis: {}", ex.getMessage());
            return true;
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

    private void deleteKey(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception ex) {
            log.warn("Unable to delete Redis key {}: {}", key, ex.getMessage());
        }
    }
}
