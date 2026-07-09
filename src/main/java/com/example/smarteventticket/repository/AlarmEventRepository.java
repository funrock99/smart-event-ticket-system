package com.example.smarteventticket.repository;

import com.example.smarteventticket.entity.AlarmEvent;
import com.example.smarteventticket.enums.AlarmSeverity;
import com.example.smarteventticket.enums.EventType;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlarmEventRepository extends JpaRepository<AlarmEvent, Long> {

    @Query("""
            select event
            from AlarmEvent event
            where (:source is null or lower(event.source) = lower(cast(:source as string)))
              and (:eventType is null or event.eventType = :eventType)
              and (:severity is null or event.severity = :severity)
            """)
    Page<AlarmEvent> search(
            @Param("source") String source,
            @Param("eventType") EventType eventType,
            @Param("severity") AlarmSeverity severity,
            Pageable pageable
    );

    long countBySeverityIn(Collection<AlarmSeverity> severities);

    @Query("SELECT e.source, COUNT(e) FROM AlarmEvent e GROUP BY e.source ORDER BY COUNT(e) DESC")
    java.util.List<Object[]> countBySource();

    @Query("SELECT DISTINCT e.severity FROM AlarmEvent e WHERE e.source = :source")
    java.util.List<AlarmSeverity> findDistinctSeveritiesBySource(@Param("source") String source);

    AlarmEvent findFirstBySourceOrderByOccurredAtDesc(String source);
}

