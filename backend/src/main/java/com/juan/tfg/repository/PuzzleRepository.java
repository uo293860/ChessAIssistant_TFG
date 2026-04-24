package com.juan.tfg.repository;

import com.juan.tfg.model.Puzzle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PuzzleRepository extends JpaRepository<Puzzle, String> {

    // Esta es la consulta "mágica" para tu TFG:
    // Busca un puzle aleatorio que contenga una temática y esté en un rango de ELO
    @Query(nativeQuery = true, value =
            "SELECT * FROM puzzles " +
                    "WHERE themes LIKE %:theme% " +
                    "AND rating BETWEEN :minRating AND :maxRating " +
                    "ORDER BY RANDOM() LIMIT 1")
    Optional<Puzzle> findRandomPuzzleByThemeAndRating(
            @Param("theme") String theme,
            @Param("minRating") int minRating,
            @Param("maxRating") int maxRating);
}
