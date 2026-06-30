package com.example.smartmaintenance.controller;

import com.example.smartmaintenance.dto.request.CreateEquipmentRequest;
import com.example.smartmaintenance.dto.request.UpdateEquipmentStatusRequest;
import com.example.smartmaintenance.dto.response.EquipmentResponse;
import com.example.smartmaintenance.service.EquipmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/equipments")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipmentResponse create(@Valid @RequestBody CreateEquipmentRequest request) {
        return equipmentService.createEquipment(request);
    }

    @GetMapping
    public List<EquipmentResponse> findAll() {
        return equipmentService.findAll();
    }

    @GetMapping("/{equipmentId}")
    public EquipmentResponse findByEquipmentId(@PathVariable String equipmentId) {
        return equipmentService.findByEquipmentId(equipmentId);
    }

    @PutMapping("/{equipmentId}/status")
    public EquipmentResponse updateStatus(
            @PathVariable String equipmentId,
            @Valid @RequestBody UpdateEquipmentStatusRequest request
    ) {
        return equipmentService.updateStatus(equipmentId, request);
    }
}

