package com.vks.interfaces.bookings.scheduler;

import com.vks.interfaces.bookings.entity.BookingEntity;
import com.vks.interfaces.bookings.entity.BookingStatus;
import com.vks.interfaces.bookings.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpiryScheduler {

    private final BookingRepository bookingRepository;

    @Scheduled(fixedDelayString = "${booking.expiry-check-ms:60000}")
    @Transactional
    public void expireStaleBookings() {
        List<BookingEntity> expired = bookingRepository.findExpiredPendingBookings(
                List.of(BookingStatus.DRAFT, BookingStatus.PAYMENT_PENDING),
                LocalDateTime.now()
        );

        if (expired.isEmpty()) {
            return;
        }

        log.info("Expiring {} stale booking(s)", expired.size());
        expired.forEach(b -> b.setStatus(BookingStatus.EXPIRED));
        bookingRepository.saveAll(expired);
    }
}
