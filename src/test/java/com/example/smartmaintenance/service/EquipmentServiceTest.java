package com.example.smartmaintenance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.smartmaintenance.dto.request.CreateEquipmentRequest;
import com.example.smartmaintenance.entity.Equipment;
import com.example.smartmaintenance.enums.EquipmentStatus;
import com.example.smartmaintenance.exception.DuplicateResourceException;
import com.example.smartmaintenance.repository.EquipmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private EquipmentService equipmentService;

    @Test
    void createEquipmentShouldSucceed() {
        CreateEquipmentRequest request = new CreateEquipmentRequest("EQP-100", "Tester", "C-Line");
        Equipment saved = new Equipment();
        saved.setId(10L);
        saved.setEquipmentId(request.equipmentId());
        saved.setName(request.name());
        saved.setFactoryArea(request.factoryArea());
        saved.setStatus(EquipmentStatus.RUNNING);

        when(equipmentRepository.existsByEquipmentId(request.equipmentId())).thenReturn(false);
        when(equipmentRepository.save(any(Equipment.class))).thenReturn(saved);

        var response = equipmentService.createEquipment(request);

        assertEquals("EQP-100", response.equipmentId());
        assertEquals(EquipmentStatus.RUNNING, response.status());
        verify(equipmentRepository).save(any(Equipment.class));
        verify(cacheService).cacheEquipmentStatus("EQP-100", EquipmentStatus.RUNNING);
        verify(cacheService).evictDashboardSummary();
    }

    @Test
    void createEquipmentShouldFailWhenDuplicate() {
        CreateEquipmentRequest request = new CreateEquipmentRequest("EQP-001", "Tester", "C-Line");
        when(equipmentRepository.existsByEquipmentId("EQP-001")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> equipmentService.createEquipment(request));
    }
}
