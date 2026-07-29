package com.vks.interfaces.eventcatalog.slot.controller;

import com.vks.common.ApiEndpoints;
import com.vks.interfaces.eventcatalog.slot.model.SlotRequest;
import com.vks.interfaces.eventcatalog.slot.model.SlotResponse;
import com.vks.interfaces.eventcatalog.slot.service.SlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiEndpoints.BASE_TENANT_ADMIN)
@RequiredArgsConstructor
public class SlotController {

    private final SlotService slotService;

    @GetMapping(ApiEndpoints.SLOTS)
    public ResponseEntity<List<SlotResponse>> getSlots(@PathVariable UUID eventId) {
        return ResponseEntity.ok(slotService.getSlotsByEvent(eventId));
    }

    @PostMapping(ApiEndpoints.SLOTS)
    public ResponseEntity<SlotResponse> createSlot(
            @PathVariable UUID eventId,
            @Valid @RequestBody SlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(slotService.createSlot(eventId, request));
    }

    @PatchMapping(ApiEndpoints.SLOTS_BY_ID)
    public ResponseEntity<SlotResponse> updateSlot(
            @PathVariable UUID eventId,
            @PathVariable UUID slotId,
            @Valid @RequestBody SlotRequest request) {
        return ResponseEntity.ok(slotService.updateSlot(eventId, slotId, request));
    }

    @DeleteMapping(ApiEndpoints.SLOTS_BY_ID)
    public ResponseEntity<Void> deleteSlot(
            @PathVariable UUID eventId,
            @PathVariable UUID slotId) {
        slotService.deleteSlot(eventId, slotId);
        return ResponseEntity.noContent().build();
    }
}
