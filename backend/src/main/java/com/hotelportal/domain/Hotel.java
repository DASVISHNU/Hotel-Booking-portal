package com.hotelportal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * A hotel with a single room category (as allowed by the brief).
 * {@code totalRooms} is the number of identical rooms that can be sold per night.
 */
@Entity
@Table(name = "hotels")
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 80)
    private String location;

    @Column(nullable = false, length = 1000)
    private String description;

    /** Comma-separated list, e.g. "Free WiFi,Pool,Breakfast". */
    @Column(nullable = false, length = 500)
    private String amenities;

    @Column(name = "room_type", nullable = false, length = 80)
    private String roomType;

    @Column(name = "price_per_night", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    @Column(name = "total_rooms", nullable = false)
    private int totalRooms;

    protected Hotel() {
        // for JPA
    }

    public Hotel(String name, String location, String description, String amenities,
                 String roomType, BigDecimal pricePerNight, int totalRooms) {
        this.name = name;
        this.location = location;
        this.description = description;
        this.amenities = amenities;
        this.roomType = roomType;
        this.pricePerNight = pricePerNight;
        this.totalRooms = totalRooms;
    }

    public List<String> getAmenityList() {
        if (amenities == null || amenities.isBlank()) {
            return List.of();
        }
        return Arrays.stream(amenities.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public String getAmenities() {
        return amenities;
    }

    public String getRoomType() {
        return roomType;
    }

    public BigDecimal getPricePerNight() {
        return pricePerNight;
    }

    public int getTotalRooms() {
        return totalRooms;
    }
}
