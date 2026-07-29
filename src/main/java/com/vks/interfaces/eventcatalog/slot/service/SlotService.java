package com.vks.interfaces.eventcatalog.slot.service;

import com.vks.interfaces.eventcatalog.slot.model.SlotRequest;
import com.vks.interfaces.eventcatalog.slot.model.SlotResponse;

import java.util.List;
import java.util.UUID;

public interface SlotService {

    List<SlotResponse> getSlotsByEvent(UUID eventId);

    SlotResponse createSlot(UUID eventId, SlotRequest request);

    SlotResponse updateSlot(UUID eventId, UUID slotId, SlotRequest request);

    void deleteSlot(UUID eventId, UUID slotId);
}
