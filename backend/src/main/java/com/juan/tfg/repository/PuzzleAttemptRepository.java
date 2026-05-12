package com.juan.tfg.repository;

import com.juan.tfg.model.PuzzleAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PuzzleAttemptRepository extends JpaRepository<PuzzleAttempt, Long> {

    @Query("""
            select attempt
            from PuzzleAttempt attempt
            where attempt.user.firebaseUid = :firebaseUid
              and attempt.isSuccessful = true
              and attempt.failedAttempts = 0
              and attempt.resultingElo is not null
            order by attempt.attemptDate asc, attempt.id asc
            """)
    List<PuzzleAttempt> findSuccessfulEloHistoryByUserId(@Param("firebaseUid") String firebaseUid);

    @Query("select count(attempt) from PuzzleAttempt attempt where attempt.user.firebaseUid = :firebaseUid")
    long countByFirebaseUid(@Param("firebaseUid") String firebaseUid);

    @Query("""
            select count(attempt)
            from PuzzleAttempt attempt
            where attempt.user.firebaseUid = :firebaseUid
            and attempt.isSuccessful = true
            and attempt.failedAttempts = 0
            """)
    long countSuccessfulByFirebaseUid(@Param("firebaseUid") String firebaseUid);
}
