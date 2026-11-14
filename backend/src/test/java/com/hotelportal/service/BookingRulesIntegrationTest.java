package com.hotelportal.service;

import com.hotelportal.domain.Hotel;
import com.hotelportal.dto.AlternativesResponse;
import com.hotelportal.dto.BookingResponse;
import com.hotelportal.dto.CreateBookingRequest;
import com.hotelportal.exception.ApiException;
import com.hotelportal.repository.BookingRepository;
import com.hotelportal.repository.HotelRepository;
import com.hotelportal.repository.RoomInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Availability, overlap, cancellation and concurrency rules, run against an in-memory database. */
@SpringBootTest
class BookingRulesIntegrationTest {

    @Autowired
    private BookingService bookingService;
    @Autowired
    private HotelService hotelService;
    @Autowired
    private AlternativesService alternativesService;
    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private RoomInventoryRepository inventoryRepository;

    private Hotel twoRoomHotel;
    private LocalDate day0;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        inventoryRepository.deleteAll();
        hotelRepository.deleteAll();
        twoRoomHotel = hotelRepository.save(new Hotel("Test Inn", "Testville", "A test hotel", "WiFi,Pool",
                "Standard Room", new BigDecimal("2000.00"), 2));
        day0 = LocalDate.now().plusDays(30);
    }

    @Test
    void bookingTotalIsCalculatedOnTheServerAndHasAReference() {
        BookingResponse booking = book(twoRoomHotel, day0, day0.plusDays(3), "asha@example.com");

        assertThat(booking.nights()).isEqualTo(3);
        assertThat(booking.totalAmount()).isEqualByComparingTo("6000.00");
        assertThat(booking.status()).isEqualTo("CONFIRMED");
        assertThat(booking.reference()).matches("TC[A-Z2-9]{8}");
    }

    @Test
    void cannotBookBeyondCapacity() {
        book(twoRoomHotel, day0, day0.plusDays(2), "a@example.com");
        book(twoRoomHotel, day0, day0.plusDays(2), "b@example.com");

        assertThatThrownBy(() -> book(twoRoomHotel, day0, day0.plusDays(2), "c@example.com"))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("NO_AVAILABILITY");
        assertThat(availability(day0, day0.plusDays(2))).isZero();
    }

    @Test
    void partialOverlapCountsAgainstCapacityButBackToBackStaysDoNot() {
        book(twoRoomHotel, day0, day0.plusDays(3), "a@example.com");
        book(twoRoomHotel, day0, day0.plusDays(3), "b@example.com");

        // Overlaps the last night (day0+2) which is full.
        assertThatThrownBy(() -> book(twoRoomHotel, day0.plusDays(2), day0.plusDays(5), "c@example.com"))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("NO_AVAILABILITY");

        // Starts on the other guests' check-out day: no shared night, so it is allowed.
        BookingResponse backToBack = book(twoRoomHotel, day0.plusDays(3), day0.plusDays(5), "c@example.com");
        assertThat(backToBack.status()).isEqualTo("CONFIRMED");
    }

    @Test
    void failedBookingDoesNotReserveAnyNight() {
        book(twoRoomHotel, day0.plusDays(2), day0.plusDays(3), "a@example.com");
        book(twoRoomHotel, day0.plusDays(2), day0.plusDays(3), "b@example.com");

        // Nights day0 and day0+1 are free but day0+2 is full -> whole booking must roll back.
        assertThatThrownBy(() -> book(twoRoomHotel, day0, day0.plusDays(3), "c@example.com"))
                .isInstanceOf(ApiException.class);

        assertThat(availability(day0, day0.plusDays(2))).isEqualTo(2);
    }

    @Test
    void cancellingReleasesTheRoomForOtherGuests() {
        BookingResponse first = book(twoRoomHotel, day0, day0.plusDays(2), "a@example.com");
        book(twoRoomHotel, day0, day0.plusDays(2), "b@example.com");
        assertThat(availability(day0, day0.plusDays(2))).isZero();

        BookingResponse cancelled = bookingService.cancel(first.reference(), "a@example.com");

        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(availability(day0, day0.plusDays(2))).isEqualTo(1);
        assertThat(book(twoRoomHotel, day0, day0.plusDays(2), "c@example.com").status()).isEqualTo("CONFIRMED");
    }

    @Test
    void cannotCancelTwice() {
        BookingResponse booking = book(twoRoomHotel, day0, day0.plusDays(1), "a@example.com");
        bookingService.cancel(booking.reference(), "a@example.com");

        assertThatThrownBy(() -> bookingService.cancel(booking.reference(), "a@example.com"))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("ALREADY_CANCELLED");
        assertThat(availability(day0, day0.plusDays(1))).isEqualTo(2);
    }

    @Test
    void lookupNeedsTheMatchingEmailButIgnoresCase() {
        BookingResponse booking = book(twoRoomHotel, day0, day0.plusDays(1), "Asha@Example.com");

        assertThat(bookingService.lookup(booking.reference().toLowerCase(), "ASHA@example.COM").reference())
                .isEqualTo(booking.reference());
        assertThatThrownBy(() -> bookingService.lookup(booking.reference(), "someone.else@example.com"))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("BOOKING_NOT_FOUND");
    }

    @Test
    void alternativesSuggestNearbyFreeDatesWhenSoldOut() {
        book(twoRoomHotel, day0, day0.plusDays(2), "a@example.com");
        book(twoRoomHotel, day0, day0.plusDays(2), "b@example.com");

        AlternativesResponse alternatives = alternativesService.suggest(twoRoomHotel.getId(), day0, day0.plusDays(2));

        assertThat(alternatives.availableForRequestedDates()).isZero();
        assertThat(alternatives.alternativeDates()).isNotEmpty();
        alternatives.alternativeDates().forEach(option -> {
            assertThat(option.nights()).isEqualTo(2);
            assertThat(availability(option.checkIn(), option.checkOut())).isPositive();
        });
    }

    @Test
    void twoGuestsRacingForTheLastRoomOnlyOneWins() throws Exception {
        Hotel oneRoomHotel = hotelRepository.save(new Hotel("Tiny Inn", "Testville", "One room only", "WiFi",
                "Single Room", new BigDecimal("1500.00"), 1));
        CountDownLatch startTogether = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        List<Future<Boolean>> results = new ArrayList<>();
        for (String email : List.of("first@example.com", "second@example.com")) {
            Callable<Boolean> attempt = () -> {
                startTogether.await();
                try {
                    book(oneRoomHotel, day0, day0.plusDays(2), email);
                    return true;
                } catch (RuntimeException e) {
                    return false;
                }
            };
            results.add(pool.submit(attempt));
        }
        startTogether.countDown();

        int successes = 0;
        for (Future<Boolean> result : results) {
            if (result.get(30, TimeUnit.SECONDS)) {
                successes++;
            }
        }
        pool.shutdown();

        assertThat(successes).isEqualTo(1);
        assertThat(hotelService.availability(oneRoomHotel.getId(), day0, day0.plusDays(2)).availableRooms()).isZero();
    }

    private BookingResponse book(Hotel hotel, LocalDate checkIn, LocalDate checkOut, String email) {
        return bookingService.create(new CreateBookingRequest(hotel.getId(), checkIn, checkOut, "Test Guest", email));
    }

    private int availability(LocalDate checkIn, LocalDate checkOut) {
        return hotelService.availability(twoRoomHotel.getId(), checkIn, checkOut).availableRooms();
    }
}
