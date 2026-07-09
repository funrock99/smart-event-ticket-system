package com.example.smarteventticket.service;

import com.example.smarteventticket.dto.request.CreateEquipmentRequest;
import com.example.smarteventticket.dto.request.UpdateEquipmentStatusRequest;
import com.example.smarteventticket.dto.response.EquipmentResponse;
import com.example.smarteventticket.entity.Equipment;
import com.example.smarteventticket.enums.EquipmentStatus;
import com.example.smarteventticket.exception.DuplicateResourceException;
import com.example.smarteventticket.exception.ResourceNotFoundException;
import com.example.smarteventticket.repository.EquipmentRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final CacheService cacheService;

    public EquipmentService(EquipmentRepository equipmentRepository, CacheService cacheService) {
        this.equipmentRepository = equipmentRepository;
        this.cacheService = cacheService;
    }

    @Transactional
    public EquipmentResponse createEquipment(CreateEquipmentRequest request) {
        if (equipmentRepository.existsByEquipmentId(request.equipmentId())) {
            throw new DuplicateResourceException("Equipment already exists: " + request.equipmentId());
        }

        Equipment equipment = new Equipment();
        equipment.setEquipmentId(request.equipmentId());
        equipment.setName(request.name());
        equipment.setFactoryArea(request.factoryArea());
        equipment.setStatus(EquipmentStatus.RUNNING);

        Equipment savedEquipment = equipmentRepository.save(equipment);
        cacheService.cacheEquipmentStatus(savedEquipment.getEquipmentId(), savedEquipment.getStatus());
        cacheService.evictDashboardSummary();
        return toResponse(savedEquipment);
    }

    @Transactional(readOnly = true)
    public List<EquipmentResponse> findAll() {
        return equipmentRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Equipment::getEquipmentId))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EquipmentResponse findByEquipmentId(String equipmentId) {
        return toResponse(getEquipmentEntity(equipmentId));
    }

    @Transactional
    public EquipmentResponse updateStatus(String equipmentId, UpdateEquipmentStatusRequest request) {
        Equipment equipment = getEquipmentEntity(equipmentId);
        equipment.setStatus(request.status());
        Equipment savedEquipment = equipmentRepository.save(equipment);
        cacheService.cacheEquipmentStatus(savedEquipment.getEquipmentId(), savedEquipment.getStatus());
        cacheService.evictDashboardSummary();
        return toResponse(savedEquipment);
    }

    @Transactional
    public void updateEquipmentStatus(String equipmentId, EquipmentStatus status) {
        Equipment equipment = getEquipmentEntity(equipmentId);
        equipment.setStatus(status);
        equipmentRepository.save(equipment);
        cacheService.cacheEquipmentStatus(equipmentId, status);
        cacheService.evictDashboardSummary();
    }

    @Transactional(readOnly = true)
    public Equipment getEquipmentEntity(String equipmentId) {
        return equipmentRepository.findByEquipmentId(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found: " + equipmentId));
    }

    private EquipmentResponse toResponse(Equipment equipment) {
        return new EquipmentResponse(
                equipment.getId(),
                equipment.getEquipmentId(),
                equipment.getName(),
                equipment.getFactoryArea(),
                equipment.getStatus(),
                equipment.getLastHeartbeatTime()
        );
    }
}

