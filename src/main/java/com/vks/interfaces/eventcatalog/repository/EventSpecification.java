package com.vks.interfaces.eventcatalog.repository;

import com.vks.interfaces.eventcatalog.entity.EventEntity;
import com.vks.interfaces.eventcatalog.model.EventSearchRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class EventSpecification {

    private EventSpecification() {}

    public static Specification<EventEntity> build(EventSearchRequest request, String tenantId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("tenantId"), tenantId));

            if (request.getEventName() != null && !request.getEventName().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("eventName")),
                        "%" + request.getEventName().toLowerCase() + "%"));
            }

            if (request.getLocation() != null && !request.getLocation().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("location")),
                        "%" + request.getLocation().toLowerCase() + "%"));
            }

            if (request.getCreatedBy() != null && !request.getCreatedBy().isBlank()) {
                predicates.add(cb.equal(root.get("createdBy"), request.getCreatedBy()));
            }

            if (request.getStartDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), request.getStartDateFrom()));
            }

            if (request.getStartDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), request.getStartDateTo()));
            }

            if (request.getEndDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), request.getEndDateFrom()));
            }

            if (request.getEndDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), request.getEndDateTo()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
