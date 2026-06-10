package com.juan.tfg.repository;

import com.juan.tfg.model.PuzzleSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PuzzleSessionRepository extends JpaRepository<PuzzleSession, Long> {

    Optional<PuzzleSession> findByIdAndUserFirebaseUid(Long id, String firebaseUid);

    List<PuzzleSession> findByUserFirebaseUidAndCompletedFalseAndFailedAttemptsGreaterThan(
            String firebaseUid,
            int failedAttempts
    );
}
