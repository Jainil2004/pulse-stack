package com.pulsestack.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "systems")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class System {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String systemId;

    private String name;

    private LocalDateTime registeredAt;

//    uncomment the below line when in production but comment it for testing
    @ManyToOne( )// fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    @JsonBackReference
    private User user;

//    adding authorization at machine level to ensure only valid authenticated and authorized machines are
//    allowed to push data to backend.
    @Column(nullable = false)
    private String authToken;

}
