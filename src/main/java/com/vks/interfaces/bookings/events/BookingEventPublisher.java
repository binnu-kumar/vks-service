package com.vks.interfaces.bookings.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.booking-events:booking.events}")
    private String bookingEventsTopic;

    public void publish(BookingEvent event) {
        String key = event.getBookingId().toString();
        kafkaTemplate.send(bookingEventsTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish booking event: bookingId={}, error={}", key, ex.getMessage());
                    } else {
                        log.info("Published booking event: type={}, bookingId={}", event.getEventType(), key);
                    }
                });
    }
}
