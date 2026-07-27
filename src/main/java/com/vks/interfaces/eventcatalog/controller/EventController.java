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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiEndpoints.BASE_TENANT_ADMIN)
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping(ApiEndpoints.EVENTS)
    public ResponseEntity<List<EventResponse>> listEvents() {
        return ResponseEntity.ok(eventService.listEvents());
    }

    @GetMapping(ApiEndpoints.EVENTS_BY_ID)
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getEvent(eventId));
    }

    @PostMapping(ApiEndpoints.EVENTS_SEARCH)
    public ResponseEntity<List<EventResponse>> searchEvents(@RequestBody EventSearchRequest request) {
        return ResponseEntity.ok(eventService.searchEvents(request));
    }

    @PostMapping(ApiEndpoints.EVENTS)
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody EventRequest request,
            Authentication authentication) {
        String createdBy = authentication != null ? authentication.getName() : "unknown";
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request, createdBy));
    }

    @PatchMapping(ApiEndpoints.EVENTS_BY_ID)
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody EventRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(eventId, request));
    }
}
