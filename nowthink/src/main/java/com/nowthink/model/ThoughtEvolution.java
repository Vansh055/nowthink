package com.nowthink.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "thought_evolution")
public class ThoughtEvolution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String theme;

    @Column(columnDefinition = "TEXT")
    private String belief;

    @Column(columnDefinition = "TEXT")
    private String sourceObservation;

    private LocalDateTime recordedAt;

    @PrePersist
    public void prePersist() {
        this.recordedAt = LocalDateTime.now();
    }
}