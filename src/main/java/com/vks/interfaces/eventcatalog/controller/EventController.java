package com.vks.interfaces.eventcatalog.controller;

import com.vks.common.ApiEndpoints;
import com.vks.interfaces.eventcatalog.model.EventRequest;
import com.vks.interfaces.eventcatalog.model.EventResponse;
import com.vks.interfaces.eventcatalog.model.EventSearchRequest;
import com.vks.interfaces.eventcatalog.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiEndpoints.BASE_TENANT_ADMIN)
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping(ApiEndpoints.EVENTS_SEARCH)
    public ResponseEntity<List<EventResponse>> searchEvents(@RequestBody EventSearchRequest request) {
        return ResponseEntity.ok(eventService.searchEvents(request));
    }

    @PostMapping(ApiEndpoints.EVENTS)
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request));
    }

    @PatchMapping(ApiEndpoints.EVENTS_BY_ID)
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable UUID eventId,
            @Valid @RequestBody EventRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(eventId, request));
    }

    @DeleteMapping(ApiEndpoints.EVENTS_BY_ID)
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID eventId) {
        eventService.deleteEvent(eventId);
        return ResponseEntity.noContent().build();
    }
}
