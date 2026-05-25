package com.juan.tfg.repository;

import com.juan.tfg.model.Puzzle;
import com.juan.tfg.model.PuzzleAttempt;
import com.juan.tfg.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class RepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PuzzleRepository puzzleRepository;

    @Autowired
    private PuzzleAttemptRepository puzzleAttemptRepository;

    @Test
    void existsByUsername_withPersistedUser() {
        // Given
        User user = buildUser("user-1", "player-one");
        entityManager.persistAndFlush(user);

        // When
        boolean result = userRepository.existsByUsername("player-one");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void existsByUsername_withMissingUsername() {
        // Given
        User user = buildUser("user-1", "player-one");
        entityManager.persistAndFlush(user);

        // When
        boolean result = userRepository.existsByUsername("missing-player");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void findAllByOrderByEloRatingDescUsernameAsc() {
        // Given
        User lowerRatedUser = buildUser("user-1", "lower");
        lowerRatedUser.setEloRating(1200);
        User higherRatedUser = buildUser("user-2", "higher");
        higherRatedUser.setEloRating(1600);
        User tiedUserFirstByUsername = buildUser("user-3", "alpha");
        tiedUserFirstByUsername.setEloRating(1400);
        User tiedUserSecondByUsername = buildUser("user-4", "zeta");
        tiedUserSecondByUsername.setEloRating(1400);
        entityManager.persist(lowerRatedUser);
        entityManager.persist(higherRatedUser);
        entityManager.persist(tiedUserSecondByUsername);
        entityManager.persist(tiedUserFirstByUsername);
        entityManager.flush();

        // When
        List<User> result = userRepository.findAllByOrderByEloRatingDescUsernameAsc();

        // Then
        assertThat(result)
                .extracting(User::getUsername)
                .containsExactly("higher", "alpha", "zeta", "lower");
        assertThat(result)
                .extracting(User::getEloRating)
                .containsExactly(1600, 1400, 1400, 1200);
    }

    @Test
    void findPuzzleByThemeAndRating() {
        // Given
        Puzzle expectedPuzzle = buildPuzzle("puzzle-1", 1250, "fork middlegame");
        Puzzle outOfThemePuzzle = buildPuzzle("puzzle-2", 1240, "mate endgame");
        Puzzle outOfRatingPuzzle = buildPuzzle("puzzle-3", 1600, "fork tactic");
        entityManager.persist(expectedPuzzle);
        entityManager.persist(outOfThemePuzzle);
        entityManager.persist(outOfRatingPuzzle);
        entityManager.flush();

        // When
        Optional<Puzzle> result = puzzleRepository.findRandomPuzzleByThemeAndRating("fork", 1200, 1300);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("puzzle-1");
        assertThat(result.get().getThemes()).contains("fork");
        assertThat(result.get().getRating()).isBetween(1200, 1300);
    }

    @Test
    void findPuzzleByThemeAndRating_shouldReturnEmpty() {
        // Given
        Puzzle puzzle = buildPuzzle("puzzle-1", 1250, "fork middlegame");
        entityManager.persistAndFlush(puzzle);

        // When
        Optional<Puzzle> result = puzzleRepository.findRandomPuzzleByThemeAndRating("mate", 1200, 1300);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findEloHistoryByUserId() {
        // Given
        User user = buildUser("user-1", "player-one");
        User otherUser = buildUser("user-2", "player-two");
        Puzzle puzzle = buildPuzzle("puzzle-1", 1200, "fork middlegame");
        entityManager.persist(user);
        entityManager.persist(otherUser);
        entityManager.persist(puzzle);

        PuzzleAttempt latestAttempt = persistAttempt(user, puzzle, true, 0, 12, 1012);
        PuzzleAttempt ignoredAttemptWithoutResultingElo = persistAttempt(user, puzzle, false, 1, -10, null);
        PuzzleAttempt earliestAttempt = persistAttempt(user, puzzle, true, 0, 15, 1027);
        PuzzleAttempt otherUserAttempt = persistAttempt(otherUser, puzzle, true, 0, 8, 1008);
        entityManager.flush();

        updateAttemptDate(latestAttempt.getId(), LocalDateTime.of(2026, 5, 15, 12, 0));
        updateAttemptDate(ignoredAttemptWithoutResultingElo.getId(), LocalDateTime.of(2026, 5, 15, 9, 0));
        updateAttemptDate(earliestAttempt.getId(), LocalDateTime.of(2026, 5, 15, 10, 0));
        updateAttemptDate(otherUserAttempt.getId(), LocalDateTime.of(2026, 5, 15, 8, 0));
        entityManager.clear();

        // When
        List<PuzzleAttempt> result = puzzleAttemptRepository.findEloHistoryByUserId("user-1");

        // Then
        assertThat(result)
                .extracting(PuzzleAttempt::getId)
                .containsExactly(earliestAttempt.getId(), latestAttempt.getId());
        assertThat(result)
                .extracting(PuzzleAttempt::getResultingElo)
                .containsExactly(1027, 1012);
    }

    @Test
    void countByFirebaseUid_shouldReturnUserAttemptCount() {
        // Given
        User user = buildUser("user-1", "player-one");
        User otherUser = buildUser("user-2", "player-two");
        Puzzle puzzle = buildPuzzle("puzzle-1", 1200, "fork middlegame");
        entityManager.persist(user);
        entityManager.persist(otherUser);
        entityManager.persist(puzzle);
        persistAttempt(user, puzzle, true, 0, 12, 1012);
        persistAttempt(user, puzzle, false, 2, -8, 1004);
        persistAttempt(otherUser, puzzle, true, 0, 10, 1010);
        entityManager.flush();

        // When
        long result = puzzleAttemptRepository.countByFirebaseUid("user-1");

        // Then
        assertThat(result).isEqualTo(2);
    }

    @Test
    void countAttemptsWithNoMistakes() {
        // Given
        User user = buildUser("user-1", "player-one");
        Puzzle puzzle = buildPuzzle("puzzle-1", 1200, "fork middlegame");
        entityManager.persist(user);
        entityManager.persist(puzzle);
        persistAttempt(user, puzzle, true, 0, 12, 1012);
        persistAttempt(user, puzzle, true, 1, 8, 1020);
        persistAttempt(user, puzzle, false, 0, -12, 1000);
        entityManager.flush();

        // When
        long result = puzzleAttemptRepository.countSuccessfulByFirebaseUid("user-1");

        // Then
        assertThat(result).isEqualTo(1);
    }

    private User buildUser(String firebaseUid, String username) {
        return User.builder()
                .firebaseUid(firebaseUid)
                .username(username)
                .email(username + "@example.com")
                .eloRating(1000)
                .build();
    }

    private Puzzle buildPuzzle(String id, int rating, String themes) {
        return Puzzle.builder()
                .id(id)
                .fen("start-fen")
                .moves("e2e4 e7e5")
                .rating(rating)
                .themes(themes)
                .gameUrl("https://lichess.org/" + id)
                .build();
    }

    private PuzzleAttempt persistAttempt(
            User user,
            Puzzle puzzle,
            boolean successful,
            int failedAttempts,
            int eloChange,
            Integer resultingElo
    ) {
        PuzzleAttempt attempt = PuzzleAttempt.builder()
                .user(user)
                .puzzle(puzzle)
                .isSuccessful(successful)
                .failedAttempts(failedAttempts)
                .hintsUsed(0)
                .eloChange(eloChange)
                .resultingElo(resultingElo)
                .build();
        entityManager.persist(attempt);
        return attempt;
    }

    private void updateAttemptDate(Long attemptId, LocalDateTime attemptDate) {
        entityManager.getEntityManager()
                .createNativeQuery("update puzzle_attempts set attempt_date = ? where id = ?")
                .setParameter(1, Timestamp.valueOf(attemptDate))
                .setParameter(2, attemptId)
                .executeUpdate();
    }
}
