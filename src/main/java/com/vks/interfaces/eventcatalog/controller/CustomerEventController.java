package com.vks.interfaces.eventcatalog.controller;

import com.vks.common.ApiEndpoints;
import com.vks.interfaces.eventcatalog.model.EventResponse;
import com.vks.interfaces.eventcatalog.service.EventService;
import com.vks.interfaces.slot.model.SlotResponse;
import com.vks.interfaces.slot.service.SlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiEndpoints.BASE_CUSTOMER)
@RequiredArgsConstructor
public class CustomerEventController {

    private final EventService eventService;
    private final SlotService slotService;

    @GetMapping(ApiEndpoints.EVENTS)
    public ResponseEntity<List<EventResponse>> listEvents() {
        return ResponseEntity.ok(eventService.listEvents());
    }

    @GetMapping(ApiEndpoints.EVENTS_BY_ID)
    public ResponseEntity<EventResponse> getEvent(@PathVariable UUID eventId) {
        return ResponseEntity.ok(eventService.getEvent(eventId));
    }

    @GetMapping(ApiEndpoints.SLOTS)
    public ResponseEntity<List<SlotResponse>> getSlots(@PathVariable UUID eventId) {
        return ResponseEntity.ok(slotService.getSlotsByEvent(eventId));
    }
}