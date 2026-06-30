package com.example.smartmaintenance.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AlarmRecentRedisContainerIntegrationTest {

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void clearRedis() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void postAlarmShouldWriteRecentAlarmToRedisAndRecentApiShouldReadIt() throws Exception {
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
                .andExpect(jsonPath("$.alarmCode").value("TEMP_HIGH"));

        List<String> cachedAlarms = redisTemplate.opsForList().range("alarm:recent", 0, -1);
        assertThat(cachedAlarms).isNotNull().hasSize(1);
        assertThat(cachedAlarms.get(0)).contains("\"equipmentId\":\"EQP-001\"");
        assertThat(cachedAlarms.get(0)).contains("\"alarmCode\":\"TEMP_HIGH\"");

        mockMvc.perform(get("/api/alarms/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].equipmentId").value("EQP-001"))
                .andExpect(jsonPath("$[0].alarmCode").value("TEMP_HIGH"))
                .andExpect(jsonPath("$[0].severity").value("HIGH"))
                .andExpect(jsonPath("$[0].message").value("Temperature exceeded threshold"));
    }
}
