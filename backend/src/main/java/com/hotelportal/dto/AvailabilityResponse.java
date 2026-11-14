package com.hotelportal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Availability plus the server-calculated price quote shown before the user confirms. */
public record AvailabilityResponse(
        Long hotelId,
        String hotelName,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal pricePerNight,
        BigDecimal totalAmount,
        int availableRooms,
        boolean available
) {
}
