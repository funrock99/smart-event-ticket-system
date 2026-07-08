package com.example.smartmaintenance.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.example.smartmaintenance.dto.request.UpdateTicketStatusRequest;
import com.example.smartmaintenance.entity.MaintenanceTicket;
import com.example.smartmaintenance.enums.TicketStatus;
import com.example.smartmaintenance.exception.InvalidStatusTransitionException;
import com.example.smartmaintenance.repository.MaintenanceTicketRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private MaintenanceTicketRepository ticketRepository;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private TicketService ticketService;

    @Test
    void updateStatusShouldRejectInvalidTransition() {
        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.setId(1L);
        ticket.setStatus(TicketStatus.OPEN);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThrows(
                InvalidStatusTransitionException.class,
                () -> ticketService.updateStatus(1L, new UpdateTicketStatusRequest(TicketStatus.RESOLVED))
        );
    }
}