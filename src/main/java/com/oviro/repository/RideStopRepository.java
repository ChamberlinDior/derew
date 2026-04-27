package com.oviro.repository;

import com.oviro.model.RideStop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RideStopRepository extends JpaRepository<RideStop, UUID> {

    List<RideStop> findByRideIdOrderByStopOrder(UUID rideId);
}
