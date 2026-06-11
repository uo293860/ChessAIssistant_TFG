package com.juan.tfg.repository;

import com.juan.tfg.model.PuzzleAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PuzzleAttemptRepository extends JpaRepository<PuzzleAttempt, Long> {

    @Query("""
            select attempt
            from PuzzleAttempt attempt
            where attempt.user.firebaseUid = :firebaseUid
              and attempt.resultingElo is not null
            order by attempt.attemptDate asc, attempt.id asc
            """)
    List<PuzzleAttempt> findEloHistoryByUserId(@Param("firebaseUid") String firebaseUid);

    @Query("select count(attempt) from PuzzleAttempt attempt where attempt.user.firebaseUid = :firebaseUid")
    long countByFirebaseUid(@Param("firebaseUid") String firebaseUid);

    @Query("""
            select count(attempt)
            from PuzzleAttempt attempt
            where attempt.user.firebaseUid = :firebaseUid
            and attempt.isSuccessful = true
            """)
    long countSuccessfulByFirebaseUid(@Param("firebaseUid") String firebaseUid);

    @Query(value = """
            select *
            from puzzle_attempts
            where user_id = :firebaseUid
              and is_successful = false
            order by random()
            limit 1
            """, nativeQuery = true)
    Optional<PuzzleAttempt> findRandomFailedAttempt(@Param("firebaseUid") String firebaseUid);

    @Query("""
            select attempt.user.firebaseUid as firebaseUid,
                   coalesce(sum(attempt.eloChange), 0) as eloChange
            from PuzzleAttempt attempt
            where attempt.attemptDate >= :startOfDay
              and attempt.resultingElo is not null
            group by attempt.user.firebaseUid
            """)
    List<UserDailyEloChange> findDailyEloChangesSince(@Param("startOfDay") LocalDateTime startOfDay);

    interface UserDailyEloChange {
        String getFirebaseUid();

        Long getEloChange();
    }
}
