package com.alphaskyport.logistics.repository;

import com.alphaskyport.logistics.model.TrackingNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackingNumberRepository extends JpaRepository<TrackingNumber, Long> {

    @Query(value = "SELECT generate_tracking_number()", nativeQuery = true)
    String generateTrackingNumber();
}
