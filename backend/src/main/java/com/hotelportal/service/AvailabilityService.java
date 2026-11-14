package com.hotelportal.service;

import com.hotelportal.domain.Hotel;
import com.hotelportal.domain.RoomInventory;
import com.hotelportal.exception.ApiException;
import com.hotelportal.repository.RoomInventoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Availability rule: a room is free for a stay only if it is free on <em>every</em> night of the stay,
 * so rooms available = totalRooms - (most rooms booked on any single night of the stay).
 */
@Service
public class AvailabilityService {

    private final RoomInventoryRepository inventoryRepository;

    public AvailabilityService(RoomInventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public int availableRooms(Hotel hotel, StayDates stay) {
        int maxBooked = inventoryRepository.findForRange(hotel.getId(), stay.checkIn(), stay.checkOut()).stream()
                .mapToInt(RoomInventory::getBookedRooms)
                .max()
                .orElse(0);
        return Math.max(0, hotel.getTotalRooms() - maxBooked);
    }

    /** hotelId -> most rooms booked on any night of the stay (hotels with no bookings are absent). */
    public Map<Long, Integer> maxBookedByHotel(StayDates stay) {
        Map<Long, Integer> result = new HashMap<>();
        for (Object[] row : inventoryRepository.findMaxBookedPerHotel(stay.checkIn(), stay.checkOut())) {
            result.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
        }
        return result;
    }

    /** date -> rooms booked that night, for nights in [from, toExclusive). */
    public Map<LocalDate, Integer> bookedByDate(Hotel hotel, LocalDate from, LocalDate toExclusive) {
        return inventoryRepository.findForRange(hotel.getId(), from, toExclusive).stream()
                .collect(Collectors.toMap(RoomInventory::getStayDate, RoomInventory::getBookedRooms));
    }

    /**
     * Reserves one room for every night of the stay, or nothing at all.
     * Must be called inside a transaction that already holds the hotel's row lock
     * ({@code HotelRepository.findByIdForUpdate}), so no other booking can change these rows meanwhile.
     */
    public void reserveOneRoom(Hotel lockedHotel, StayDates stay) {
        Map<LocalDate, RoomInventory> existing = inventoryRepository
                .findForRangeForUpdate(lockedHotel.getId(), stay.checkIn(), stay.checkOut()).stream()
                .collect(Collectors.toMap(RoomInventory::getStayDate, Function.identity()));

        List<RoomInventory> rows = new ArrayList<>();
        for (LocalDate night : stay.nightDates()) {
            RoomInventory row = existing.get(night);
            if (row == null) {
                row = new RoomInventory(lockedHotel, night, lockedHotel.getTotalRooms());
            }
            if (row.getAvailableRooms() <= 0) {
                throw ApiException.conflict("NO_AVAILABILITY", String.format(
                        "Sorry, %s is fully booked on the night of %s. Please try different dates.",
                        lockedHotel.getName(), night));
            }
            rows.add(row);
        }
        rows.forEach(RoomInventory::reserveOne);
        inventoryRepository.saveAll(rows);
    }

    /** Gives back one room for each night of a cancelled stay. Caller must hold the hotel's row lock. */
    public void releaseOneRoom(Hotel lockedHotel, LocalDate checkIn, LocalDate checkOut) {
        List<RoomInventory> rows = inventoryRepository.findForRangeForUpdate(lockedHotel.getId(), checkIn, checkOut);
        rows.forEach(RoomInventory::releaseOne);
        inventoryRepository.saveAll(rows);
    }
}
