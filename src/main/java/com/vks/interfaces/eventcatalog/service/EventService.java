package com.vks.interfaces.eventcatalog.service;

import com.vks.interfaces.eventcatalog.model.EventRequest;
import com.vks.interfaces.eventcatalog.model.EventResponse;
import com.vks.interfaces.eventcatalog.model.EventSearchRequest;

import java.util.List;
import java.util.UUID;

public interface EventService {

    List<EventResponse> listEvents();

    EventResponse getEvent(UUID eventId);

    EventResponse createEvent(EventRequest request);

    EventResponse updateEvent(UUID eventId, EventRequest request);

    void deleteEvent(UUID eventId);

    List<EventResponse> searchEvents(EventSearchRequest request);
}
