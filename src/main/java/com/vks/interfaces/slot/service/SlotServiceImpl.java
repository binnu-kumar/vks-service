package com.vks.interfaces.slot.service;

import com.vks.common.exception.ResourceNotFoundException;
import com.vks.interfaces.eventcatalog.entity.EventEntity;
import com.vks.interfaces.eventcatalog.repository.EventRepository;
import com.vks.interfaces.slot.entity.SlotEntity;
import com.vks.interfaces.slot.model.SlotRequest;
import com.vks.interfaces.slot.model.SlotResponse;
import com.vks.interfaces.slot.repository.SlotRepository;
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
public class SlotServiceImpl implements SlotService {

    private final SlotRepository slotRepository;
    private final EventRepository eventRepository;
    private final SecurityContextService securityContextService;

    @Override
    public List<SlotResponse> getSlotsByEvent(UUID eventId) {
        AuthenticatedUser currentUser = securityContextService.currentUser();
        log.info("Fetching slots for event id: {}", eventId);
        validateEvent(eventId, currentUser.tenantId());
        return slotRepository.findByEventEventIdAndEventTenantId(eventId, currentUser.tenantId()).stream().map(this::toResponse).toList();
    }

    @Override
    public SlotResponse createSlot(UUID eventId, SlotRequest request) {
        AuthenticatedUser currentUser = securityContextService.currentUser();
        log.info("Creating slot for event id: {}", eventId);
        EventEntity event = validateEvent(eventId, currentUser.tenantId());
        SlotEntity slot = new SlotEntity();
        slot.setEvent(event);
        mapToEntity(request, slot);
        return toResponse(slotRepository.save(slot));
    }

    @Override
    public SlotResponse updateSlot(UUID eventId, UUID slotId, SlotRequest request) {
        AuthenticatedUser currentUser = securityContextService.currentUser();
        log.info("Updating slot id: {} for event id: {}", slotId, eventId);
        validateEvent(eventId, currentUser.tenantId());
        SlotEntity slot = findSlotForEvent(slotId, eventId, currentUser.tenantId());
        mapToEntity(request, slot);
        return toResponse(slotRepository.save(slot));
    }

    @Override
    public void deleteSlot(UUID eventId, UUID slotId) {
        AuthenticatedUser currentUser = securityContextService.currentUser();
        log.info("Deleting slot id: {} for event id: {}", slotId, eventId);
        validateEvent(eventId, currentUser.tenantId());
        SlotEntity slot = findSlotForEvent(slotId, eventId, currentUser.tenantId());
        slotRepository.delete(slot);
    }

    private EventEntity validateEvent(UUID eventId, String tenantId) {
        return eventRepository.findByEventIdAndTenantId(eventId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
    }

    private SlotEntity findSlotForEvent(UUID slotId, UUID eventId, String tenantId) {
        return slotRepository.findBySlotIdAndEventEventIdAndEventTenantId(slotId, eventId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Slot not found with id: " + slotId + " for event id: " + eventId));
    }

    private void mapToEntity(SlotRequest request, SlotEntity slot) {
        slot.setSlotDate(request.getSlotDate());
        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        slot.setPrice(request.getPrice());
        slot.setCapacity(request.getCapacity());
    }

    private SlotResponse toResponse(SlotEntity slot) {
        return new SlotResponse(
                slot.getSlotId(),
                slot.getEvent().getEventId(),
                slot.getSlotDate(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getPrice(),
                slot.getCapacity(),
                slot.getCreatedAt(),
                slot.getUpdatedAt()
        );
    }
}
