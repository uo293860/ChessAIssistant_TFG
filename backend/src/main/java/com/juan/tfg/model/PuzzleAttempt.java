package com.juan.tfg.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "puzzle_attempts")
@Data @NoArgsConstructor
@AllArgsConstructor @Builder
public class PuzzleAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación con el Usuario
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Relación con el Puzle
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puzzle_id", nullable = false)
    private Puzzle puzzle;

    @Column(name = "is_successful", nullable = false)
    @Builder.Default
    private Boolean isSuccessful = false;

    @Column(name = "failed_attempts")
    @Builder.Default
    private int failedAttempts = 0;

    @Column(name = "hints_used")
    @Builder.Default
    private Integer hintsUsed = 0;

    @CreationTimestamp
    @Column(name = "attempt_date", updatable = false)
    private LocalDateTime attemptDate;
}
