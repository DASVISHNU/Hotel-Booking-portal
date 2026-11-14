package com.hotelportal.repository;

import com.hotelportal.domain.Hotel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HotelRepository extends JpaRepository<Hotel, Long> {

    List<Hotel> findAllByOrderByNameAsc();

    List<Hotel> findByLocationContainingIgnoreCaseOrderByNameAsc(String location);

    List<Hotel> findByLocationIgnoreCaseAndIdNot(String location, Long id);

    @Query("select distinct h.location from Hotel h order by h.location")
    List<String> findDistinctLocations();

    /**
     * SELECT ... FOR UPDATE on the hotel row. Every booking/cancellation for a hotel takes this
     * lock first, so changes to that hotel's availability happen one at a time.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Hotel h where h.id = :id")
    Optional<Hotel> findByIdForUpdate(@Param("id") Long id);
}
