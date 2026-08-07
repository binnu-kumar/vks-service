package com.vks.interfaces.eventcatalog.service;

import com.vks.interfaces.eventcatalog.entity.EventEntity;
import com.vks.interfaces.eventcatalog.model.EventRequest;
import com.vks.interfaces.eventcatalog.model.EventResponse;
import com.vks.interfaces.eventcatalog.model.EventSearchRequest;
import com.vks.interfaces.eventcatalog.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventServiceImpl eventService;

    @Test
    void listEventsMapsRepositoryResults() {
        EventEntity event = eventEntity();
        when(eventRepository.findAll()).thenReturn(List.of(event));

        List<EventResponse> responses = eventService.listEvents();

        assertEquals(1, responses.size());
        assertEquals(event.getEventId(), responses.get(0).getEventId());
        assertEquals(event.getEventName(), responses.get(0).getEventName());
    }

    @Test
    void getEventThrowsWhenMissing() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> eventService.getEvent(eventId));

        assertEquals("Event not found with id: " + eventId, exception.getMessage());
    }

    @Test
    void createEventMapsRequestAndCreatedBy() {
        EventRequest request = eventRequest();
        EventEntity saved = eventEntity();
        when(eventRepository.save(any(EventEntity.class))).thenReturn(saved);

        EventResponse response = eventService.createEvent(request, "9876543210");

        ArgumentCaptor<EventEntity> captor = ArgumentCaptor.forClass(EventEntity.class);
        verify(eventRepository).save(captor.capture());
        EventEntity persisted = captor.getValue();
        assertEquals("9876543210", persisted.getCreatedBy());
        assertEquals(request.getEventName(), persisted.getEventName());
        assertEquals(saved.getEventId(), response.getEventId());
    }

    @Test
    void updateEventMapsUpdatedFields() {
        UUID eventId = UUID.randomUUID();
        EventEntity existing = eventEntity();
        existing.setEventId(eventId);
        EventRequest request = eventRequest();
        request.setEventName("Updated Event");
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(existing));
        when(eventRepository.save(existing)).thenReturn(existing);

        EventResponse response = eventService.updateEvent(eventId, request);

        assertEquals("Updated Event", existing.getEventName());
        assertEquals(eventId, response.getEventId());
    }

    @Test
    void deleteEventDeletesResolvedEntity() {
        UUID eventId = UUID.randomUUID();
        EventEntity existing = eventEntity();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(existing));

        eventService.deleteEvent(eventId);

        verify(eventRepository).delete(existing);
    }

    @Test
    void searchEventsUsesSpecificationAndMapsResults() {
        EventSearchRequest request = new EventSearchRequest();
        request.setEventName("Tech");
        EventEntity event = eventEntity();
        when(eventRepository.findAll(any(Specification.class))).thenReturn(List.of(event));

        List<EventResponse> responses = eventService.searchEvents(request);

        assertEquals(1, responses.size());
        assertEquals(event.getEventId(), responses.get(0).getEventId());
    }

    private EventEntity eventEntity() {
        EventEntity entity = new EventEntity();
        entity.setEventId(UUID.randomUUID());
        entity.setEventName("Tech Summit");
        entity.setDescription("Conference");
        entity.setLocation("Bangalore");
        entity.setStartDate(LocalDateTime.of(2025, 9, 1, 9, 0));
        entity.setEndDate(LocalDateTime.of(2025, 9, 1, 18, 0));
        entity.setCreatedBy("9876543210");
        entity.setCreatedAt(LocalDateTime.of(2025, 7, 1, 10, 0));
        entity.setUpdatedAt(LocalDateTime.of(2025, 7, 1, 10, 0));
        return entity;
    }

    private EventRequest eventRequest() {
        EventRequest request = new EventRequest();
        request.setEventName("Tech Summit");
        request.setDescription("Conference");
        request.setLocation("Bangalore");
        request.setStartDate(LocalDateTime.of(2025, 9, 1, 9, 0));
        request.setEndDate(LocalDateTime.of(2025, 9, 1, 18, 0));
        return request;
    }
}