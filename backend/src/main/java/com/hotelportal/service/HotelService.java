package com.hotelportal.service;

import com.hotelportal.domain.Hotel;
import com.hotelportal.dto.AvailabilityResponse;
import com.hotelportal.dto.HotelResponse;
import com.hotelportal.exception.ApiException;
import com.hotelportal.repository.HotelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class HotelService {

    private final HotelRepository hotelRepository;
    private final AvailabilityService availabilityService;
    private final Clock clock;

    public HotelService(HotelRepository hotelRepository, AvailabilityService availabilityService, Clock clock) {
        this.hotelRepository = hotelRepository;
        this.availabilityService = availabilityService;
        this.clock = clock;
    }

    public List<String> locations() {
        return hotelRepository.findDistinctLocations();
    }

    /** Browse/search. Dates are optional; when given, each hotel includes availability and total price. */
    public List<HotelResponse> search(String location, LocalDate checkIn, LocalDate checkOut) {
        List<Hotel> hotels = (location == null || location.isBlank())
                ? hotelRepository.findAllByOrderByNameAsc()
                : hotelRepository.findByLocationContainingIgnoreCaseOrderByNameAsc(location.trim());

        if (checkIn == null && checkOut == null) {
            return hotels.stream().map(HotelResponse::from).toList();
        }
        StayDates stay = StayDates.validate(checkIn, checkOut, today());
        Map<Long, Integer> maxBooked = availabilityService.maxBookedByHotel(stay);
        return hotels.stream()
                .map(h -> HotelResponse.forStay(h, stay,
                        Math.max(0, h.getTotalRooms() - maxBooked.getOrDefault(h.getId(), 0))))
                .toList();
    }

    public HotelResponse getHotel(Long id, LocalDate checkIn, LocalDate checkOut) {
        Hotel hotel = findHotel(id);
        if (checkIn == null && checkOut == null) {
            return HotelResponse.from(hotel);
        }
        StayDates stay = StayDates.validate(checkIn, checkOut, today());
        return HotelResponse.forStay(hotel, stay, availabilityService.availableRooms(hotel, stay));
    }

    public AvailabilityResponse availability(Long id, LocalDate checkIn, LocalDate checkOut) {
        Hotel hotel = findHotel(id);
        StayDates stay = StayDates.validate(checkIn, checkOut, today());
        int available = availabilityService.availableRooms(hotel, stay);
        return new AvailabilityResponse(hotel.getId(), hotel.getName(), stay.checkIn(), stay.checkOut(),
                stay.nights(), hotel.getPricePerNight(),
                PriceCalculator.total(hotel.getPricePerNight(), stay.nights()),
                available, available > 0);
    }

    private Hotel findHotel(Long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("HOTEL_NOT_FOUND", "Hotel " + id + " was not found."));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
