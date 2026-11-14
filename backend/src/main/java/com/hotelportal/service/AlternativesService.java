package com.hotelportal.service;

import com.hotelportal.domain.Hotel;
import com.hotelportal.dto.AlternativesResponse;
import com.hotelportal.dto.AlternativesResponse.DateOption;
import com.hotelportal.dto.HotelResponse;
import com.hotelportal.exception.ApiException;
import com.hotelportal.repository.HotelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Surprise feature — "Flexible dates & nearby stays".
 * When a hotel is sold out for the chosen dates, instead of a dead end the guest gets:
 *  1. the closest shifted date windows (same number of nights, up to ±7 days) at this hotel that are free, and
 *  2. other hotels in the same location that have rooms for the original dates, cheapest first.
 */
@Service
@Transactional(readOnly = true)
public class AlternativesService {

    static final int SEARCH_WINDOW_DAYS = 7;
    static final int MAX_DATE_OPTIONS = 3;
    static final int MAX_HOTEL_OPTIONS = 3;

    private final HotelRepository hotelRepository;
    private final AvailabilityService availabilityService;
    private final Clock clock;

    public AlternativesService(HotelRepository hotelRepository, AvailabilityService availabilityService, Clock clock) {
        this.hotelRepository = hotelRepository;
        this.availabilityService = availabilityService;
        this.clock = clock;
    }

    public AlternativesResponse suggest(Long hotelId, LocalDate checkIn, LocalDate checkOut) {
        LocalDate today = LocalDate.now(clock);
        StayDates stay = StayDates.validate(checkIn, checkOut, today);
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> ApiException.notFound("HOTEL_NOT_FOUND", "Hotel " + hotelId + " was not found."));
        int nights = stay.nights();

        // One query covers every window we might try.
        LocalDate windowStart = later(today, stay.checkIn().minusDays(SEARCH_WINDOW_DAYS));
        LocalDate windowEnd = stay.checkOut().plusDays(SEARCH_WINDOW_DAYS);
        Map<LocalDate, Integer> booked = availabilityService.bookedByDate(hotel, windowStart, windowEnd);

        int requestedAvailable = freeRooms(hotel, booked, stay.checkIn(), stay.checkOut());

        // Try ±1 day, then ±2 days ... so the closest options come first.
        List<DateOption> dateOptions = new ArrayList<>();
        for (int shift = 1; shift <= SEARCH_WINDOW_DAYS && dateOptions.size() < MAX_DATE_OPTIONS; shift++) {
            for (int direction : new int[]{-1, 1}) {
                if (dateOptions.size() >= MAX_DATE_OPTIONS) {
                    break;
                }
                int daysShifted = shift * direction;
                LocalDate in = stay.checkIn().plusDays(daysShifted);
                if (in.isBefore(today)) {
                    continue;
                }
                LocalDate out = in.plusDays(nights);
                int free = freeRooms(hotel, booked, in, out);
                if (free > 0) {
                    dateOptions.add(new DateOption(in, out, nights, free,
                            PriceCalculator.total(hotel.getPricePerNight(), nights), daysShifted));
                }
            }
        }
        dateOptions.sort(Comparator.comparing(DateOption::checkIn));

        Map<Long, Integer> maxBooked = availabilityService.maxBookedByHotel(stay);
        List<HotelResponse> similarHotels = hotelRepository
                .findByLocationIgnoreCaseAndIdNot(hotel.getLocation(), hotel.getId()).stream()
                .map(h -> HotelResponse.forStay(h, stay,
                        Math.max(0, h.getTotalRooms() - maxBooked.getOrDefault(h.getId(), 0))))
                .filter(h -> h.availableRooms() > 0)
                .sorted(Comparator.comparing(HotelResponse::pricePerNight))
                .limit(MAX_HOTEL_OPTIONS)
                .toList();

        return new AlternativesResponse(hotel.getId(), stay.checkIn(), stay.checkOut(), requestedAvailable,
                dateOptions, similarHotels);
    }

    private static int freeRooms(Hotel hotel, Map<LocalDate, Integer> booked, LocalDate in, LocalDate out) {
        int maxBooked = in.datesUntil(out).mapToInt(d -> booked.getOrDefault(d, 0)).max().orElse(0);
        return Math.max(0, hotel.getTotalRooms() - maxBooked);
    }

    private static LocalDate later(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }
}
