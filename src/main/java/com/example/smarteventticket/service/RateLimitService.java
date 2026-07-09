package com.example.smarteventticket.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final CacheService cacheService;
    private final long requestsPerMinute;

    public RateLimitService(
            CacheService cacheService,
            @Value("${app.redis.rate-limit-per-minute:60}") long requestsPerMinute
    ) {
        this.cacheService = cacheService;
        this.requestsPerMinute = requestsPerMinute;
    }

    public boolean allow(String source) {
        long count = cacheService.incrementRateRequestCount(source);
        return count == 0 || count <= requestsPerMinute;
    }

    public void recordRateLimitedRequest() {
        cacheService.incrementRateLimitedEventCount();
    }
}
