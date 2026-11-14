package com.hotelportal.repository;

import com.hotelportal.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByReference(String reference);

    @Query("select b from Booking b join fetch b.hotel where b.reference = :reference")
    Optional<Booking> findByReferenceWithHotel(@Param("reference") String reference);
}
