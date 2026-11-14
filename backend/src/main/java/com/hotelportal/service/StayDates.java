package com.hotelportal.service;

import com.hotelportal.exception.ApiException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * A validated stay. Nights are the half-open range [checkIn, checkOut):
 * a guest occupies a room on the night of every date from check-in up to, but not including, check-out.
 * So a stay ending on the 5th and another starting on the 5th never compete for the same room-night.
 */
public record StayDates(LocalDate checkIn, LocalDate checkOut) {

    public static final int MAX_NIGHTS = 30;
    public static final int MAX_DAYS_AHEAD = 365;

    public static StayDates validate(LocalDate checkIn, LocalDate checkOut, LocalDate today) {
        if (checkIn == null || checkOut == null) {
            throw ApiException.badRequest("DATES_REQUIRED", "Please select both check-in and check-out dates.");
        }
        if (checkIn.isBefore(today)) {
            throw ApiException.badRequest("CHECK_IN_IN_PAST", "Check-in date cannot be in the past.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw ApiException.badRequest("INVALID_DATE_RANGE", "Check-out date must be after the check-in date.");
        }
        if (ChronoUnit.DAYS.between(checkIn, checkOut) > MAX_NIGHTS) {
            throw ApiException.badRequest("STAY_TOO_LONG", "A single booking can be for at most " + MAX_NIGHTS + " nights.");
        }
        if (checkIn.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            throw ApiException.badRequest("TOO_FAR_AHEAD", "Bookings can be made up to " + MAX_DAYS_AHEAD + " days in advance.");
        }
        return new StayDates(checkIn, checkOut);
    }

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** The dates whose nights this stay occupies (check-out day excluded). */
    public List<LocalDate> nightDates() {
        return checkIn.datesUntil(checkOut).toList();
    }
}
