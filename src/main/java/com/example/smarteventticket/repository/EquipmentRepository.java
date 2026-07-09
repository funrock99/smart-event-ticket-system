package com.example.smarteventticket.repository;

import com.example.smarteventticket.entity.Equipment;
import com.example.smarteventticket.enums.EquipmentStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    boolean existsByEquipmentId(String equipmentId);

    Optional<Equipment> findByEquipmentId(String equipmentId);

    long countByStatus(EquipmentStatus status);
}


