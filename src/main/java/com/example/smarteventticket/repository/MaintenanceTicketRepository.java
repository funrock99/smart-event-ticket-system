package com.example.smarteventticket.repository;

import com.example.smarteventticket.entity.MaintenanceTicket;
import com.example.smarteventticket.enums.EventType;
import com.example.smarteventticket.enums.TicketPriority;
import com.example.smarteventticket.enums.TicketStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, Long> {

    Optional<MaintenanceTicket> findByTicketNo(String ticketNo);

    Optional<MaintenanceTicket> findTopBySourceAndEventTypeAndBusinessKeyOrderByCreatedAtDesc(
            String source,
            EventType eventType,
            String businessKey
    );

    long countByStatus(TicketStatus status);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
            select ticket
            from MaintenanceTicket ticket
            where (:status is null or ticket.status = :status)
              and (:priority is null or ticket.priority = :priority)
            """)
    Page<MaintenanceTicket> search(
            @Param("status") TicketStatus status,
            @Param("priority") TicketPriority priority,
            Pageable pageable
    );
}

