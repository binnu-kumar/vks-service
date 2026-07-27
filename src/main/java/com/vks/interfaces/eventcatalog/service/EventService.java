package com.vks.interfaces.eventcatalog.service;

import com.vks.interfaces.eventcatalog.model.EventRequest;
import com.vks.interfaces.eventcatalog.model.EventResponse;
import com.vks.interfaces.eventcatalog.model.EventSearchRequest;

import java.util.List;

public interface EventService {

    List<EventResponse> listEvents();

    EventResponse getEvent(Long eventId);

    EventResponse createEvent(EventRequest request, String createdBy);

    EventResponse updateEvent(Long eventId, EventRequest request);

    List<EventResponse> searchEvents(EventSearchRequest request);
}
