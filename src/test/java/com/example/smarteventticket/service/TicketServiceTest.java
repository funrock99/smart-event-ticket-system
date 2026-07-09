package com.example.smarteventticket.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.example.smarteventticket.dto.request.UpdateTicketStatusRequest;
import com.example.smarteventticket.entity.MaintenanceTicket;
import com.example.smarteventticket.enums.TicketStatus;
import com.example.smarteventticket.exception.InvalidStatusTransitionException;
import com.example.smarteventticket.repository.MaintenanceTicketRepository;
import com.example.smarteventticket.repository.TicketStatusHistoryRepository;
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
    private TicketStatusHistoryRepository ticketStatusHistoryRepository;

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
