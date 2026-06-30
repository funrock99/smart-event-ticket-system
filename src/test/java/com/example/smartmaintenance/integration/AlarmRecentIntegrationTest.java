package com.example.smartmaintenance.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AlarmRecentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void postAlarmThenGetRecentShouldReturnCreatedAlarm() throws Exception {
        mockMvc.perform(post("/api/alarms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "equipmentId": "EQP-001",
                                  "alarmCode": "TEMP_HIGH",
                                  "severity": "HIGH",
                                  "message": "Temperature exceeded threshold"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.equipmentId").value("EQP-001"))
                .andExpect(jsonPath("$.alarmCode").value("TEMP_HIGH"))
                .andExpect(jsonPath("$.ticketNo").exists());

        mockMvc.perform(get("/api/alarms/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].equipmentId").value("EQP-001"))
                .andExpect(jsonPath("$[0].alarmCode").value("TEMP_HIGH"))
                .andExpect(jsonPath("$[0].severity").value("HIGH"))
                .andExpect(jsonPath("$[0].message").value("Temperature exceeded threshold"));
    }
}
