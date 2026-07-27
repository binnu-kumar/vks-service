package com.vks.interfaces.eventcatalog.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventSearchRequest {

    private String eventName;
    private String location;
    private String createdBy;
    private LocalDateTime startDateFrom;
    private LocalDateTime startDateTo;
    private LocalDateTime endDateFrom;
    private LocalDateTime endDateTo;
}
