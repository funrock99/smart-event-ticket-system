package com.example.smartmaintenance.repository;

import com.example.smartmaintenance.entity.MaintenanceTicket;
import com.example.smartmaintenance.enums.EventType;
import com.example.smartmaintenance.enums.TicketStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, Long> {

    Optional<MaintenanceTicket> findByTicketNo(String ticketNo);

    Optional<MaintenanceTicket> findTopBySourceAndEventTypeAndBusinessKeyOrderByCreatedAtDesc(
            String source,
            EventType eventType,
            String businessKey
    );

    long countByStatus(TicketStatus status);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}