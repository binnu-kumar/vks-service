package com.vks.interfaces.slot.service;

import com.vks.interfaces.eventcatalog.entity.EventEntity;
import com.vks.interfaces.eventcatalog.repository.EventRepository;
import com.vks.interfaces.slot.entity.SlotEntity;
import com.vks.interfaces.slot.model.SlotRequest;
import com.vks.interfaces.slot.model.SlotResponse;
import com.vks.interfaces.slot.repository.SlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlotServiceImpl implements SlotService {

    private final SlotRepository slotRepository;
    private final EventRepository eventRepository;

    @Override
    public List<SlotResponse> getSlotsByEvent(UUID eventId) {
        log.info("Fetching slots for event id: {}", eventId);
        validateEvent(eventId);
        return slotRepository.findByEventEventId(eventId).stream().map(this::toResponse).toList();
    }

    @Override
    public SlotResponse createSlot(UUID eventId, SlotRequest request) {
        log.info("Creating slot for event id: {}", eventId);
        EventEntity event = validateEvent(eventId);
        SlotEntity slot = new SlotEntity();
        slot.setEvent(event);
        mapToEntity(request, slot);
        return toResponse(slotRepository.save(slot));
    }

    @Override
    public SlotResponse updateSlot(UUID eventId, UUID slotId, SlotRequest request) {
        log.info("Updating slot id: {} for event id: {}", slotId, eventId);
        validateEvent(eventId);
        SlotEntity slot = findSlotForEvent(slotId, eventId);
        mapToEntity(request, slot);
        return toResponse(slotRepository.save(slot));
    }

    @Override
    public void deleteSlot(UUID eventId, UUID slotId) {
        log.info("Deleting slot id: {} for event id: {}", slotId, eventId);
        validateEvent(eventId);
        SlotEntity slot = findSlotForEvent(slotId, eventId);
        slotRepository.delete(slot);
    }

    private EventEntity validateEvent(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
    }

    private SlotEntity findSlotForEvent(UUID slotId, UUID eventId) {
        return slotRepository.findBySlotIdAndEventEventId(slotId, eventId)
                .orElseThrow(() -> new RuntimeException(
                        "Slot not found with id: " + slotId + " for event id: " + eventId));
    }

    private void mapToEntity(SlotRequest request, SlotEntity slot) {
        slot.setSlotDate(request.getSlotDate());
        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        slot.setPrice(request.getPrice());
    }

    private SlotResponse toResponse(SlotEntity slot) {
        return new SlotResponse(
                slot.getSlotId(),
                slot.getEvent().getEventId(),
                slot.getSlotDate(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getPrice(),
                slot.getCreatedAt(),
                slot.getUpdatedAt()
        );
    }
}
