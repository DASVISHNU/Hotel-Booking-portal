package com.hotelportal.service;

import com.hotelportal.domain.Booking;
import com.hotelportal.domain.BookingStatus;
import com.hotelportal.domain.Hotel;
import com.hotelportal.dto.BookingResponse;
import com.hotelportal.dto.CreateBookingRequest;
import com.hotelportal.exception.ApiException;
import com.hotelportal.repository.BookingRepository;
import com.hotelportal.repository.HotelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class BookingService {

    /** No 0/O or 1/I so references are easy to read out over the phone. */
    private static final char[] REFERENCE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final String REFERENCE_PREFIX = "TC";
    private static final int REFERENCE_RANDOM_LENGTH = 8;

    private final HotelRepository hotelRepository;
    private final BookingRepository bookingRepository;
    private final AvailabilityService availabilityService;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public BookingService(HotelRepository hotelRepository, BookingRepository bookingRepository,
                          AvailabilityService availabilityService, Clock clock) {
        this.hotelRepository = hotelRepository;
        this.bookingRepository = bookingRepository;
        this.availabilityService = availabilityService;
        this.clock = clock;
    }

    /**
     * Books one room. Flow: validate dates -> lock hotel row -> check & reserve every night -> save booking.
     * Any failure rolls the whole transaction back, so availability is never left half-updated.
     */
    @Transactional
    public BookingResponse create(CreateBookingRequest request) {
        StayDates stay = StayDates.validate(request.checkIn(), request.checkOut(), today());

        Hotel hotel = hotelRepository.findByIdForUpdate(request.hotelId())
                .orElseThrow(() -> ApiException.notFound("HOTEL_NOT_FOUND", "The selected hotel was not found."));

        availabilityService.reserveOneRoom(hotel, stay);

        BigDecimal total = PriceCalculator.total(hotel.getPricePerNight(), stay.nights());
        Booking booking = new Booking(newReference(), hotel, request.guestName().trim(),
                normalizeEmail(request.email()), stay.checkIn(), stay.checkOut(), stay.nights(),
                hotel.getPricePerNight(), total, LocalDateTime.now(clock));

        return BookingResponse.from(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public BookingResponse lookup(String reference, String email) {
        return BookingResponse.from(findOwnedBooking(reference, email));
    }

    @Transactional
    public BookingResponse cancel(String reference, String email) {
        Booking booking = findOwnedBooking(reference, email);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw ApiException.conflict("ALREADY_CANCELLED", "This booking has already been cancelled.");
        }
        if (booking.getCheckIn().isBefore(today())) {
            throw ApiException.conflict("CANCELLATION_CLOSED",
                    "This stay has already started, so it can no longer be cancelled online.");
        }

        Hotel hotel = hotelRepository.findByIdForUpdate(booking.getHotel().getId())
                .orElseThrow(() -> ApiException.notFound("HOTEL_NOT_FOUND", "The hotel for this booking was not found."));
        availabilityService.releaseOneRoom(hotel, booking.getCheckIn(), booking.getCheckOut());

        booking.cancel(LocalDateTime.now(clock));
        // Flush now so a version conflict (simultaneous cancel) surfaces here and rolls back the release too.
        return BookingResponse.from(bookingRepository.saveAndFlush(booking));
    }

    /**
     * Reference + email must both match. A wrong email returns the same "not found" as a wrong reference,
     * so the API never confirms that a reference exists to someone who doesn't own it.
     */
    private Booking findOwnedBooking(String reference, String email) {
        String ref = reference == null ? "" : reference.trim().toUpperCase(Locale.ROOT);
        String mail = normalizeEmail(email);
        return bookingRepository.findByReferenceWithHotel(ref)
                .filter(b -> b.getGuestEmail().equalsIgnoreCase(mail))
                .orElseThrow(() -> ApiException.notFound("BOOKING_NOT_FOUND",
                        "We couldn't find a booking with that reference and email address."));
    }

    private String newReference() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(REFERENCE_PREFIX);
            for (int i = 0; i < REFERENCE_RANDOM_LENGTH; i++) {
                sb.append(REFERENCE_ALPHABET[random.nextInt(REFERENCE_ALPHABET.length)]);
            }
            String candidate = sb.toString();
            if (!bookingRepository.existsByReference(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique booking reference");
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
