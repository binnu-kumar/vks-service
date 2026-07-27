package com.vks.interfaces.eventcatalog.service;

import com.vks.interfaces.eventcatalog.entity.EventEntity;
import com.vks.interfaces.eventcatalog.model.EventRequest;
import com.vks.interfaces.eventcatalog.model.EventResponse;
import com.vks.interfaces.eventcatalog.model.EventSearchRequest;
import com.vks.interfaces.eventcatalog.repository.EventRepository;
import com.vks.interfaces.eventcatalog.repository.EventSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;

    @Override
    public List<EventResponse> listEvents() {
        log.info("Fetching all events");
        return eventRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public EventResponse getEvent(Long eventId) {
        log.info("Fetching event id: {}", eventId);
        return eventRepository.findById(eventId)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
    }

    @Override
    public EventResponse createEvent(EventRequest request, String createdBy) {
        log.info("Creating event: {} by {}", request.getEventName(), createdBy);
        EventEntity entity = new EventEntity();
        mapToEntity(request, entity);
        entity.setCreatedBy(createdBy);
        return toResponse(eventRepository.save(entity));
    }

    @Override
    public EventResponse updateEvent(Long eventId, EventRequest request) {
        log.info("Updating event id: {}", eventId);
        EventEntity entity = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
        mapToEntity(request, entity);
        return toResponse(eventRepository.save(entity));
    }

    @Override
    public List<EventResponse> searchEvents(EventSearchRequest request) {
        log.info("Searching events with filters: {}", request);
        return eventRepository.findAll(EventSpecification.build(request))
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
                entity.getId(),
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
