package com.example.smartmaintenance.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Smart Maintenance Ticket System API",
                version = "v1",
                description = "Equipment alarm, maintenance ticket, and dashboard APIs",
                contact = @Contact(name = "Demo Project")
        ),
        servers = @Server(url = "/", description = "Default Server")
)
public class OpenApiConfig {
}

