package com.hotelportal.repository;

import com.hotelportal.domain.RoomInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RoomInventoryRepository extends JpaRepository<RoomInventory, Long> {

    /** Inventory rows for nights in [from, toExclusive). */
    @Query("""
            select i from RoomInventory i
            where i.hotel.id = :hotelId and i.stayDate >= :from and i.stayDate < :toExclusive
            order by i.stayDate
            """)
    List<RoomInventory> findForRange(@Param("hotelId") Long hotelId,
                                     @Param("from") LocalDate from,
                                     @Param("toExclusive") LocalDate toExclusive);

    /**
     * Same rows, read with SELECT ... FOR UPDATE. A locking read always sees the latest committed values,
     * even under MySQL's REPEATABLE READ snapshot, so the counts we increment/decrement are never stale.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i from RoomInventory i
            where i.hotel.id = :hotelId and i.stayDate >= :from and i.stayDate < :toExclusive
            order by i.stayDate
            """)
    List<RoomInventory> findForRangeForUpdate(@Param("hotelId") Long hotelId,
                                              @Param("from") LocalDate from,
                                              @Param("toExclusive") LocalDate toExclusive);

    /** For each hotel: the most rooms booked on any single night in [from, toExclusive). Rows: [hotelId, maxBooked]. */
    @Query("""
            select i.hotel.id, max(i.bookedRooms) from RoomInventory i
            where i.stayDate >= :from and i.stayDate < :toExclusive
            group by i.hotel.id
            """)
    List<Object[]> findMaxBookedPerHotel(@Param("from") LocalDate from,
                                         @Param("toExclusive") LocalDate toExclusive);
}
