package com.vks.interfaces.eventcatalog.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventRequest {

    @NotBlank(message = "Event name is required")
    private String eventName;

    private String description;

    private String location;

    private LocalDateTime startDate;

    private LocalDateTime endDate;
}
