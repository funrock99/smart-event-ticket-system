package com.example.smartmaintenance.repository;

import com.example.smartmaintenance.entity.AlarmEvent;
import com.example.smartmaintenance.enums.AlarmSeverity;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmEventRepository extends JpaRepository<AlarmEvent, Long> {

    long countBySeverityIn(Collection<AlarmSeverity> severities);
}

