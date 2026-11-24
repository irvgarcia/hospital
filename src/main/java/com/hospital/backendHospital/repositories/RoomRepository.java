package com.hospital.backendHospital.repositories;

import com.hospital.backendHospital.models.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<Room> findAllByOrderByNameAsc();

    List<Room> findByAvailableOrderByNameAsc(boolean available);

    @Query("select coalesce(sum(r.capacity), 0) from Room r")
    long sumCapacity();
}
