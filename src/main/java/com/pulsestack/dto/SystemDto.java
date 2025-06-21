package com.pulsestack.dto;

import com.pulsestack.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemDto {
    private String systemId;
    private String name;
    private LocalDateTime registeredAt;
    private String username;
    private String authToken; // to be only returned at the time of registration.

    public SystemDto(String systemId, String name, LocalDateTime registeredAt, String username) {
        this.systemId = systemId;
        this.name = name;
        this.registeredAt = registeredAt;
        this.username = username;
    }
}

