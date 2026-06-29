package com.juan.tfg.repository;

import com.juan.tfg.model.PuzzleSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PuzzleSessionRepository extends JpaRepository<PuzzleSession, Long> {

    /**
     * Finds a puzzle session by identifier and owning Firebase user.
     *
     * @param id the puzzle session identifier.
     * @param firebaseUid the Firebase user identifier.
     * @return the matching puzzle session, or an empty result when it does not belong to the user.
     */
    Optional<PuzzleSession> findByIdAndUserFirebaseUid(Long id, String firebaseUid);

    /**
     * Finds incomplete user sessions whose failed-attempt count is above a threshold.
     *
     * @param firebaseUid the Firebase user identifier.
     * @param failedAttempts the exclusive lower bound for failed attempts.
     * @return matching incomplete puzzle sessions.
     */
    List<PuzzleSession> findByUserFirebaseUidAndCompletedFalseAndFailedAttemptsGreaterThan(
            String firebaseUid,
            int failedAttempts
    );
}
