package com.juan.tfg.repository;

import com.juan.tfg.model.PuzzleAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PuzzleAttemptRepository extends JpaRepository<PuzzleAttempt, Long> {

    /**
     * Finds the chronological Elo-changing attempts for a user.
     *
     * @param firebaseUid the Firebase user identifier.
     * @return attempts that have a resulting Elo value, ordered by date and identifier.
     */
    @Query("""
            select attempt
            from PuzzleAttempt attempt
            where attempt.user.firebaseUid = :firebaseUid
              and attempt.resultingElo is not null
            order by attempt.attemptDate asc, attempt.id asc
            """)
    List<PuzzleAttempt> findEloHistoryByUserId(@Param("firebaseUid") String firebaseUid);

    /**
     * Counts all puzzle attempts made by a user.
     *
     * @param firebaseUid the Firebase user identifier.
     * @return the total number of puzzle attempts.
     */
    @Query("select count(attempt) from PuzzleAttempt attempt where attempt.user.firebaseUid = :firebaseUid")
    long countByFirebaseUid(@Param("firebaseUid") String firebaseUid);

    /**
     * Counts successful puzzle attempts made by a user.
     *
     * @param firebaseUid the Firebase user identifier.
     * @return the total number of successful attempts.
     */
    @Query("""
            select count(attempt)
            from PuzzleAttempt attempt
            where attempt.user.firebaseUid = :firebaseUid
            and attempt.isSuccessful = true
            """)
    long countSuccessfulByFirebaseUid(@Param("firebaseUid") String firebaseUid);

    /**
     * Finds one random failed puzzle attempt for a user.
     *
     * @param firebaseUid the Firebase user identifier.
     * @return a random failed attempt, or an empty result when none exists.
     */
    @Query(value = """
            select *
            from puzzle_attempts
            where user_id = :firebaseUid
              and is_successful = false
            order by random()
            limit 1
            """, nativeQuery = true)
    Optional<PuzzleAttempt> findRandomFailedAttempt(@Param("firebaseUid") String firebaseUid);

    /**
     * Aggregates Elo changes per user since the provided start-of-day timestamp.
     *
     * @param startOfDay the lower timestamp bound for attempts.
     * @return daily Elo changes grouped by Firebase UID.
     */
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
        /**
         * Returns the Firebase UID associated with the aggregated Elo change.
         *
         * @return the Firebase user identifier.
         */
        String getFirebaseUid();

        /**
         * Returns the aggregated Elo change for the user.
         *
         * @return the Elo change total.
         */
        Long getEloChange();
    }
}
