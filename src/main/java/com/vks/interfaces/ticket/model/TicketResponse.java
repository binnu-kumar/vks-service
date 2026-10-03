package com.vks.interfaces.ticket.model;

import com.vks.interfaces.ticket.entity.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketResponse {

    private UUID ticketId;
    private String ticketNumber;
    private UUID bookingId;
    private UUID eventId;
    private UUID slotId;
    private String qrPayload;
    private String qrCodeBase64;
    private TicketStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
