package com.juan.tfg.repository;

import com.juan.tfg.model.PuzzleAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PuzzleAttemptRepository extends JpaRepository<PuzzleAttempt, Long> {

    @Query("""
            select attempt
            from PuzzleAttempt attempt
            where attempt.user.firebaseUid = :firebaseUid
              and attempt.isSuccessful = true
              and attempt.resultingElo is not null
            order by attempt.attemptDate asc, attempt.id asc
            """)
    List<PuzzleAttempt> findSuccessfulEloHistoryByUserId(String firebaseUid);
}
