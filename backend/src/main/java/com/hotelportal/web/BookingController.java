package com.hotelportal.web;

import com.hotelportal.dto.BookingLookupRequest;
import com.hotelportal.dto.BookingResponse;
import com.hotelportal.dto.CancelBookingRequest;
import com.hotelportal.dto.CreateBookingRequest;
import com.hotelportal.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody CreateBookingRequest request) {
        return bookingService.create(request);
    }

    @PostMapping("/lookup")
    public BookingResponse lookup(@Valid @RequestBody BookingLookupRequest request) {
        return bookingService.lookup(request.reference(), request.email());
    }

    @PostMapping("/{reference}/cancel")
    public BookingResponse cancel(@PathVariable String reference, @Valid @RequestBody CancelBookingRequest request) {
        return bookingService.cancel(reference, request.email());
    }
}
