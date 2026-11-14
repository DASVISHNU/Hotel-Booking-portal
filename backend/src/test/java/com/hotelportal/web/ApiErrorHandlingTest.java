package com.hotelportal.web;

import com.hotelportal.domain.Hotel;
import com.hotelportal.repository.BookingRepository;
import com.hotelportal.repository.HotelRepository;
import com.hotelportal.repository.RoomInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Checks that the API returns the consistent JSON error shape for invalid input. */
@SpringBootTest
@AutoConfigureMockMvc
class ApiErrorHandlingTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private RoomInventoryRepository inventoryRepository;

    private Hotel hotel;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        inventoryRepository.deleteAll();
        hotelRepository.deleteAll();
        hotel = hotelRepository.save(new Hotel("Api Inn", "Testville", "desc", "WiFi",
                "Standard Room", new BigDecimal("2500.00"), 3));
    }

    @Test
    void invalidBookingBodyReturnsFieldErrors() throws Exception {
        String body = """
                {"hotelId": %d, "checkIn": "%s", "checkOut": "%s", "guestName": "", "email": "not-an-email"}
                """.formatted(hotel.getId(), LocalDate.now().plusDays(10), LocalDate.now().plusDays(12));

        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("guestName")))
                .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("email")));
    }

    @Test
    void pastCheckInIsRejectedWithClearMessage() throws Exception {
        mvc.perform(get("/api/hotels/{id}/availability", hotel.getId())
                        .param("checkIn", LocalDate.now().minusDays(2).toString())
                        .param("checkOut", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHECK_IN_IN_PAST"))
                .andExpect(jsonPath("$.message").value("Check-in date cannot be in the past."));
    }

    @Test
    void badDateFormatIsRejected() throws Exception {
        mvc.perform(get("/api/hotels/{id}/availability", hotel.getId())
                        .param("checkIn", "14-11-2026")
                        .param("checkOut", "2026-11-20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    void unknownHotelReturns404() throws Exception {
        mvc.perform(get("/api/hotels/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
    }

    @Test
    void successfulBookingReturns201WithServerTotal() throws Exception {
        String body = """
                {"hotelId": %d, "checkIn": "%s", "checkOut": "%s", "guestName": "Asha", "email": "asha@example.com"}
                """.formatted(hotel.getId(), LocalDate.now().plusDays(10), LocalDate.now().plusDays(12));

        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(5000.00))
                .andExpect(jsonPath("$.nights").value(2))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }
}
