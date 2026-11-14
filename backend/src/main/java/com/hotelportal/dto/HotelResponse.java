package com.hotelportal.dto;

import com.hotelportal.domain.Hotel;
import com.hotelportal.service.PriceCalculator;
import com.hotelportal.service.StayDates;

import java.math.BigDecimal;
import java.util.List;

/**
 * Hotel as shown to the user. The stay fields (availableRooms, nights, totalAmount)
 * are only filled in when the request included check-in and check-out dates.
 */
public record HotelResponse(
        Long id,
        String name,
        String location,
        String description,
        List<String> amenities,
        String roomType,
        BigDecimal pricePerNight,
        int totalRooms,
        Integer availableRooms,
        Integer nights,
        BigDecimal totalAmount
) {
    public static HotelResponse from(Hotel h) {
        return new HotelResponse(h.getId(), h.getName(), h.getLocation(), h.getDescription(), h.getAmenityList(),
                h.getRoomType(), h.getPricePerNight(), h.getTotalRooms(), null, null, null);
    }

    public static HotelResponse forStay(Hotel h, StayDates stay, int availableRooms) {
        return new HotelResponse(h.getId(), h.getName(), h.getLocation(), h.getDescription(), h.getAmenityList(),
                h.getRoomType(), h.getPricePerNight(), h.getTotalRooms(), availableRooms, stay.nights(),
                PriceCalculator.total(h.getPricePerNight(), stay.nights()));
    }
}
