package com.vks.interfaces.slot.service;

import com.vks.interfaces.slot.model.SlotRequest;
import com.vks.interfaces.slot.model.SlotResponse;

import java.util.List;
import java.util.UUID;

public interface SlotService {

    List<SlotResponse> getSlotsByEvent(UUID eventId);

    SlotResponse createSlot(UUID eventId, SlotRequest request);

    SlotResponse updateSlot(UUID eventId, UUID slotId, SlotRequest request);

    void deleteSlot(UUID eventId, UUID slotId);
}
