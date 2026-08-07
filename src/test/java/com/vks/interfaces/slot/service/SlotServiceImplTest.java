package com.vks.interfaces.slot.service;

import com.vks.interfaces.eventcatalog.entity.EventEntity;
import com.vks.interfaces.eventcatalog.repository.EventRepository;
import com.vks.interfaces.slot.entity.SlotEntity;
import com.vks.interfaces.slot.model.SlotRequest;
import com.vks.interfaces.slot.model.SlotResponse;
import com.vks.interfaces.slot.repository.SlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlotServiceImplTest {

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private SlotServiceImpl slotService;

    @Test
    void getSlotsByEventValidatesEventAndMapsSlots() {
        UUID eventId = UUID.randomUUID();
        EventEntity event = event(eventId);
        SlotEntity slot = slot(event, UUID.randomUUID());
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(slotRepository.findByEventEventId(eventId)).thenReturn(List.of(slot));

        List<SlotResponse> responses = slotService.getSlotsByEvent(eventId);

        assertEquals(1, responses.size());
        assertEquals(slot.getPrice(), responses.get(0).getPrice());
        assertEquals(eventId, responses.get(0).getEventId());
    }

    @Test
    void createSlotMapsRequestAndSaves() {
        UUID eventId = UUID.randomUUID();
        EventEntity event = event(eventId);
        SlotRequest request = slotRequest();
        SlotEntity saved = slot(event, UUID.randomUUID());
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(slotRepository.save(any(SlotEntity.class))).thenReturn(saved);

        SlotResponse response = slotService.createSlot(eventId, request);

        ArgumentCaptor<SlotEntity> captor = ArgumentCaptor.forClass(SlotEntity.class);
        verify(slotRepository).save(captor.capture());
        SlotEntity persisted = captor.getValue();
        assertEquals(request.getPrice(), persisted.getPrice());
        assertEquals(event, persisted.getEvent());
        assertEquals(saved.getSlotId(), response.getSlotId());
    }

    @Test
    void updateSlotThrowsWhenSlotDoesNotBelongToEvent() {
        UUID eventId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        EventEntity event = event(eventId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(slotRepository.findBySlotIdAndEventEventId(slotId, eventId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> slotService.updateSlot(eventId, slotId, slotRequest()));

        assertEquals("Slot not found with id: " + slotId + " for event id: " + eventId, exception.getMessage());
    }

    @Test
    void deleteSlotDeletesResolvedSlot() {
        UUID eventId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        EventEntity event = event(eventId);
        SlotEntity slot = slot(event, slotId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(slotRepository.findBySlotIdAndEventEventId(slotId, eventId)).thenReturn(Optional.of(slot));

        slotService.deleteSlot(eventId, slotId);

        verify(slotRepository).delete(slot);
    }

    private EventEntity event(UUID eventId) {
        EventEntity event = new EventEntity();
        event.setEventId(eventId);
        return event;
    }

    private SlotEntity slot(EventEntity event, UUID slotId) {
        SlotEntity slot = new SlotEntity();
        slot.setSlotId(slotId);
        slot.setEvent(event);
        slot.setSlotDate(LocalDate.of(2025, 9, 1));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(10, 30));
        slot.setPrice(new BigDecimal("499.00"));
        slot.setCreatedAt(LocalDateTime.of(2025, 7, 1, 10, 0));
        slot.setUpdatedAt(LocalDateTime.of(2025, 7, 1, 10, 0));
        return slot;
    }

    private SlotRequest slotRequest() {
        SlotRequest request = new SlotRequest();
        request.setSlotDate(LocalDate.of(2025, 9, 1));
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 30));
        request.setPrice(new BigDecimal("499.00"));
        return request;
    }
}