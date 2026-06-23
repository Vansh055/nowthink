package com.nowthink.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "discoveries")
public class Discovery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String claim;

    @Column(columnDefinition = "TEXT")
    private String evidenceFor;

    @Column(columnDefinition = "TEXT")
    private String evidenceAgainst;

    private Integer confidenceScore;
    private String status;
    private String discoveryType;
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}