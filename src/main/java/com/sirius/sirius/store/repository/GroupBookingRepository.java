package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.reservation.GroupBookingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GroupBookingRepository extends JpaRepository<GroupBookingEntity, Long> {
    @Query(value = """
            select g from GroupBookingEntity g
            where (:hotelId is null or g.hotel.id = :hotelId)
              and (:query = '' or lower(g.reference) like lower(concat('%', :query, '%'))
                   or lower(g.groupName) like lower(concat('%', :query, '%'))
                   or lower(coalesce(g.organizerName, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(g.contactPhone, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(g.contactEmail, '')) like lower(concat('%', :query, '%')))
              and g.checkIn >= :fromDate
              and g.checkIn < :toDate
            """,
            countQuery = """
            select count(g) from GroupBookingEntity g
            where (:hotelId is null or g.hotel.id = :hotelId)
              and (:query = '' or lower(g.reference) like lower(concat('%', :query, '%'))
                   or lower(g.groupName) like lower(concat('%', :query, '%'))
                   or lower(coalesce(g.organizerName, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(g.contactPhone, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(g.contactEmail, '')) like lower(concat('%', :query, '%')))
              and g.checkIn >= :fromDate
              and g.checkIn < :toDate
            """)
    Page<GroupBookingEntity> search(@Param("hotelId") Long hotelId, @Param("query") String query,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate, Pageable pageable);

    @EntityGraph(attributePaths = {"hotel", "reservations"})
    Optional<GroupBookingEntity> findWithReservationsById(Long id);
}
