package com.hotelportal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

/**
 * How many rooms of a hotel are booked for one night (the night that starts on {@code stayDate}).
 * Rows are created lazily the first time a night is booked; a missing row means nothing is booked.
 */
@Entity
@Table(name = "room_inventory",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_hotel_date", columnNames = {"hotel_id", "stay_date"}))
public class RoomInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(name = "stay_date", nullable = false)
    private LocalDate stayDate;

    @Column(name = "total_rooms", nullable = false)
    private int totalRooms;

    @Column(name = "booked_rooms", nullable = false)
    private int bookedRooms;

    protected RoomInventory() {
        // for JPA
    }

    public RoomInventory(Hotel hotel, LocalDate stayDate, int totalRooms) {
        this.hotel = hotel;
        this.stayDate = stayDate;
        this.totalRooms = totalRooms;
        this.bookedRooms = 0;
    }

    public int getAvailableRooms() {
        return Math.max(0, totalRooms - bookedRooms);
    }

    public void reserveOne() {
        if (bookedRooms >= totalRooms) {
            throw new IllegalStateException("No rooms left on " + stayDate);
        }
        bookedRooms++;
    }

    public void releaseOne() {
        if (bookedRooms > 0) {
            bookedRooms--;
        }
    }

    public Long getId() {
        return id;
    }

    public Hotel getHotel() {
        return hotel;
    }

    public LocalDate getStayDate() {
        return stayDate;
    }

    public int getTotalRooms() {
        return totalRooms;
    }

    public int getBookedRooms() {
        return bookedRooms;
    }
}
