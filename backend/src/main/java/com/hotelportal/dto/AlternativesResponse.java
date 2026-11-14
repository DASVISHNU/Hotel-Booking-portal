package com.hotelportal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Surprise feature: "Flexible dates & nearby stays" suggestions when the chosen dates don't work. */
public record AlternativesResponse(
        Long hotelId,
        LocalDate requestedCheckIn,
        LocalDate requestedCheckOut,
        int availableForRequestedDates,
        List<DateOption> alternativeDates,
        List<HotelResponse> similarHotels
) {
    /** Same length of stay, shifted by {@code daysShifted} days (negative = earlier). */
    public record DateOption(
            LocalDate checkIn,
            LocalDate checkOut,
            int nights,
            int availableRooms,
            BigDecimal totalAmount,
            int daysShifted
    ) {
    }
}
