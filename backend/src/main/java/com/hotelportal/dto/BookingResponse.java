package com.hotelportal.dto;

import com.hotelportal.domain.Booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingResponse(
        String reference,
        String status,
        Long hotelId,
        String hotelName,
        String hotelLocation,
        String guestName,
        String email,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal pricePerNight,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        LocalDateTime cancelledAt
) {
    public static BookingResponse from(Booking b) {
        return new BookingResponse(b.getReference(), b.getStatus().name(), b.getHotel().getId(),
                b.getHotel().getName(), b.getHotel().getLocation(), b.getGuestName(), b.getGuestEmail(),
                b.getCheckIn(), b.getCheckOut(), b.getNights(), b.getPricePerNight(), b.getTotalAmount(),
                b.getCreatedAt(), b.getCancelledAt());
    }
}
