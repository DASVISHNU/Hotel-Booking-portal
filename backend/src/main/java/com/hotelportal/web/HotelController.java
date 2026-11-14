package com.hotelportal.web;

import com.hotelportal.dto.AlternativesResponse;
import com.hotelportal.dto.AvailabilityResponse;
import com.hotelportal.dto.HotelResponse;
import com.hotelportal.service.AlternativesService;
import com.hotelportal.service.HotelService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    private final HotelService hotelService;
    private final AlternativesService alternativesService;

    public HotelController(HotelService hotelService, AlternativesService alternativesService) {
        this.hotelService = hotelService;
        this.alternativesService = alternativesService;
    }

    /** Browse all hotels, or search by location; add dates to get availability and total price. */
    @GetMapping
    public List<HotelResponse> search(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return hotelService.search(location, checkIn, checkOut);
    }

    @GetMapping("/locations")
    public List<String> locations() {
        return hotelService.locations();
    }

    @GetMapping("/{id}")
    public HotelResponse get(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return hotelService.getHotel(id, checkIn, checkOut);
    }

    /** Rooms left for the stay plus the backend-calculated total, shown before the user confirms. */
    @GetMapping("/{id}/availability")
    public AvailabilityResponse availability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return hotelService.availability(id, checkIn, checkOut);
    }

    /** Surprise feature: nearby dates and similar hotels when the chosen stay is sold out. */
    @GetMapping("/{id}/alternatives")
    public AlternativesResponse alternatives(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return alternativesService.suggest(id, checkIn, checkOut);
    }
}
