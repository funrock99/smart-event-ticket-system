package com.example.smarteventticket.service;

import com.example.smarteventticket.dto.response.DashboardSummaryResponse;
import com.example.smarteventticket.dto.response.SourceRankingResponse;
import com.example.smarteventticket.enums.TicketStatus;
import java.util.List;
import com.example.smarteventticket.repository.AlarmEventRepository;
import com.example.smarteventticket.repository.MaintenanceTicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final MaintenanceTicketRepository ticketRepository;
    private final AlarmEventRepository alarmEventRepository;
    private final CacheService cacheService;

    public DashboardService(
            MaintenanceTicketRepository ticketRepository,
            AlarmEventRepository alarmEventRepository,
            CacheService cacheService
    ) {
        this.ticketRepository = ticketRepository;
        this.alarmEventRepository = alarmEventRepository;
        this.cacheService = cacheService;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        return cacheService.getDashboardSummary().orElseGet(() -> {
            long validEvents = alarmEventRepository.count();
            long duplicatedEvents = cacheService.getDuplicateEventCount();
            long rateLimitedEvents = cacheService.getRateLimitedEventCount();
            long idempotentReplayedEvents = cacheService.getIdempotentReplayCount();
            DashboardSummaryResponse response = new DashboardSummaryResponse(
                    validEvents + duplicatedEvents + rateLimitedEvents + idempotentReplayedEvents,
                    validEvents,
                    duplicatedEvents,
                    rateLimitedEvents,
                    idempotentReplayedEvents,
                    ticketRepository.countByStatus(TicketStatus.OPEN),
                    ticketRepository.countByStatus(TicketStatus.PROCESSING),
                    ticketRepository.countByStatus(TicketStatus.RESOLVED),
                    ticketRepository.countByStatus(TicketStatus.CLOSED)
            );
            cacheService.cacheDashboardSummary(response);
            return response;
        });
    }

    @Transactional(readOnly = true)
    public List<SourceRankingResponse> getSourceRankings() {
        return alarmEventRepository.countBySource().stream().map(row -> {
            String source = (String) row[0];
            long count = ((Number) row[1]).longValue();

            var latestEvent = alarmEventRepository.findFirstBySourceOrderByOccurredAtDesc(source);
            var severities = alarmEventRepository.findDistinctSeveritiesBySource(source);

            String highestSeverity = severities.stream()
                    .max(Enum::compareTo)
                    .map(Enum::name)
                    .orElse("LOW");

            return new SourceRankingResponse(
                    source,
                    count,
                    latestEvent != null ? latestEvent.getEventType().name() : null,
                    highestSeverity,
                    latestEvent != null ? latestEvent.getOccurredAt() : null
            );
        }).toList();
    }
}
