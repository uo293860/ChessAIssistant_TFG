package com.juan.tfg.repository;

import com.juan.tfg.model.Puzzle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PuzzleRepository extends JpaRepository<Puzzle, String> {

    /**
     * Finds a random puzzle for a theme within the requested rating interval.
     *
     * @param theme the Lichess theme identifier.
     * @param minRating the inclusive minimum puzzle rating.
     * @param maxRating the inclusive maximum puzzle rating.
     * @return a matching random puzzle, or an empty result when none exists.
     */
    @Query(nativeQuery = true, value =
            "SELECT * FROM puzzles " +
                    "WHERE (' ' || COALESCE(themes, '') || ' ') LIKE ('% ' || :theme || ' %') " +
                    "AND rating BETWEEN :minRating AND :maxRating " +
                    "ORDER BY RANDOM() LIMIT 1")
    Optional<Puzzle> findRandomPuzzleByThemeAndRating(
            @Param("theme") String theme,
            @Param("minRating") int minRating,
            @Param("maxRating") int maxRating);

    /**
     * Finds a random puzzle for a theme regardless of rating.
     *
     * @param theme the Lichess theme identifier.
     * @return a matching random puzzle, or an empty result when none exists.
     */
    @Query(nativeQuery = true, value =
            "SELECT * FROM puzzles " +
                    "WHERE (' ' || COALESCE(themes, '') || ' ') LIKE ('% ' || :theme || ' %') " +
                    "ORDER BY RANDOM() LIMIT 1")
    Optional<Puzzle> findRandomPuzzleByTheme(@Param("theme") String theme);

    /**
     * Finds a random puzzle within the requested rating interval.
     *
     * @param minRating the inclusive minimum puzzle rating.
     * @param maxRating the inclusive maximum puzzle rating.
     * @return a matching random puzzle, or an empty result when none exists.
     */
    @Query(nativeQuery = true, value =
            "SELECT * FROM puzzles " +
                    "WHERE rating BETWEEN :minRating AND :maxRating " +
                    "ORDER BY RANDOM() LIMIT 1")
    Optional<Puzzle> findRandomPuzzleByRating(
            @Param("minRating") int minRating,
            @Param("maxRating") int maxRating);
}
