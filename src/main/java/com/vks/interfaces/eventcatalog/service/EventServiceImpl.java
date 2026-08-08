package com.vks.interfaces.eventcatalog.service;

import com.vks.common.exception.ResourceNotFoundException;
import com.vks.interfaces.eventcatalog.entity.EventEntity;
import com.vks.interfaces.eventcatalog.model.EventRequest;
import com.vks.interfaces.eventcatalog.model.EventResponse;
import com.vks.interfaces.eventcatalog.model.EventSearchRequest;
import com.vks.interfaces.eventcatalog.repository.EventRepository;
import com.vks.interfaces.eventcatalog.repository.EventSpecification;
import com.vks.security.AuthenticatedUser;
import com.vks.security.SecurityContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final SecurityContextService securityContextService;

    @Override
    public List<EventResponse> listEvents() {
        AuthenticatedUser currentUser = securityContextService.currentUser();
        log.info("Fetching all events");
        return eventRepository.findAllByTenantIdOrderByStartDateAsc(currentUser.tenantId()).stream().map(this::toResponse).toList();
    }

    @Override
    public EventResponse getEvent(UUID eventId) {
        AuthenticatedUser currentUser = securityContextService.currentUser();
        log.info("Fetching event id: {}", eventId);
        return eventRepository.findByEventIdAndTenantId(eventId, currentUser.tenantId())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
    }

    @Override
    public EventResponse createEvent(EventRequest request) {
        AuthenticatedUser currentUser = securityContextService.currentUser();
        log.info("Creating event: {} by {}", request.getEventName(), currentUser.userId());
        EventEntity entity = new EventEntity();
        mapToEntity(request, entity);
        entity.setCreatedBy(currentUser.userId());
        entity.setTenantId(currentUser.tenantId());
        return toResponse(eventRepository.save(entity));
    }

    @Override
    public EventResponse updateEvent(UUID eventId, EventRequest request) {
        AuthenticatedUser currentUser = securityContextService.currentUser();
        log.info("Updating event id: {}", eventId);
        EventEntity entity = eventRepository.findByEventIdAndTenantId(eventId, currentUser.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        mapToEntity(request, entity);
        return toResponse(eventRepository.save(entity));
    }

    @Override
    public void deleteEvent(UUID eventId) {
        AuthenticatedUser currentUser = securityContextService.currentUser();
        log.info("Deleting event id: {}", eventId);
        EventEntity entity = eventRepository.findByEventIdAndTenantId(eventId, currentUser.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        eventRepository.delete(entity);
    }

    @Override
    public List<EventResponse> searchEvents(EventSearchRequest request) {
        AuthenticatedUser currentUser = securityContextService.currentUser();
        log.info("Searching events with filters: {}", request);
        return eventRepository.findAll(EventSpecification.build(request, currentUser.tenantId()))
                .stream().map(this::toResponse).toList();
    }

    private void mapToEntity(EventRequest request, EventEntity entity) {
        entity.setEventName(request.getEventName());
        entity.setDescription(request.getDescription());
        entity.setLocation(request.getLocation());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
    }

    private EventResponse toResponse(EventEntity entity) {
        return new EventResponse(
                entity.getEventId(),
                entity.getEventName(),
                entity.getDescription(),
                entity.getLocation(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
