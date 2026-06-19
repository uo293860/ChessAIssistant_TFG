package com.juan.tfg.repository;

import com.juan.tfg.model.Puzzle;
import com.juan.tfg.model.PuzzleAttempt;
import com.juan.tfg.model.User;
import org.junit.jupiter.api.DisplayName;
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
import static org.assertj.core.api.Assertions.tuple;

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
    @DisplayName("Finds an existing username")
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
    @DisplayName("Returns false when the username does not exist")
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
    @DisplayName("Orders users by Elo descending and username ascending")
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
    @DisplayName("Finds a puzzle matching both theme and rating range")
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
    @DisplayName("Returns empty when no puzzle matches the theme and rating range")
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
    @DisplayName("Finds any themed puzzle inside the requested rating range")
    void findPuzzleByRating_returnsAnyThemeInRatingRange() {
        // Given
        Puzzle expectedPuzzle = buildPuzzle("puzzle-1", 1250, "pin endgame");
        Puzzle outOfRatingPuzzle = buildPuzzle("puzzle-2", 1500, "skewer opening");
        entityManager.persist(expectedPuzzle);
        entityManager.persist(outOfRatingPuzzle);
        entityManager.flush();

        // When
        Optional<Puzzle> result = puzzleRepository.findRandomPuzzleByRating(1200, 1300);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("puzzle-1");
    }

    @Test
    @DisplayName("Finds a themed puzzle regardless of rating range")
    void findPuzzleByTheme_returnsThemeMatchOR() {
        // Given
        Puzzle expectedPuzzle = buildPuzzle("puzzle-1", 1800, "fork middlegame");
        Puzzle outOfThemePuzzle = buildPuzzle("puzzle-2", 1250, "pin endgame");
        entityManager.persist(expectedPuzzle);
        entityManager.persist(outOfThemePuzzle);
        entityManager.flush();

        // When
        Optional<Puzzle> result = puzzleRepository.findRandomPuzzleByTheme("fork");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("puzzle-1");
    }

    @Test
    @DisplayName("Matches puzzle themes by whole tokens only")
    void findPuzzleByThemeAndRating_matchesWholeTheme() {
        // Given
        Puzzle mateInTwoPuzzle = buildPuzzle("puzzle-1", 1250, "mateIn2 middlegame");
        entityManager.persistAndFlush(mateInTwoPuzzle);

        // When
        Optional<Puzzle> result = puzzleRepository.findRandomPuzzleByThemeAndRating("mate", 1200, 1300);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Returns Elo history ordered by attempt date for one user")
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
    @DisplayName("Counts all puzzle attempts for a user")
    void countByFirebaseUid() {
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
    @DisplayName("Counts successful puzzle attempts for a user")
    void countSuccessfulAttempts() {
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
        assertThat(result).isEqualTo(2);
    }

    @Test
    @DisplayName("Finds an unsolved failed attempt for a user")
    void findRandomFailedAttempt() {
        // Given
        User user = buildUser("user-1", "player-one");
        User otherUser = buildUser("user-2", "player-two");
        Puzzle puzzle = buildPuzzle("puzzle-1", 1200, "fork middlegame");
        Puzzle otherPuzzle = buildPuzzle("puzzle-2", 1210, "fork endgame");
        entityManager.persist(user);
        entityManager.persist(otherUser);
        entityManager.persist(puzzle);
        entityManager.persist(otherPuzzle);

        PuzzleAttempt failedAttempt = persistAttempt(user, puzzle, false, 1, -16, 984);
        persistAttempt(user, puzzle, true, 0, 12, 1012);
        persistAttempt(user, otherPuzzle, true, 1, -10, 990);
        persistAttempt(otherUser, puzzle, false, 1, -10, 990);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<PuzzleAttempt> result = puzzleAttemptRepository.findRandomFailedAttempt("user-1");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(failedAttempt.getId());
    }

    @Test
    @DisplayName("Groups daily Elo changes by user since the provided date")
    void findDailyEloChanges() {
        // Given
        User user = buildUser("user-1", "player-one");
        User otherUser = buildUser("user-2", "player-two");
        Puzzle puzzle = buildPuzzle("puzzle-1", 1200, "fork middlegame");
        entityManager.persist(user);
        entityManager.persist(otherUser);
        entityManager.persist(puzzle);

        PuzzleAttempt userMorningAttempt = persistAttempt(user, puzzle, true, 0, 12, 1012);
        PuzzleAttempt userAfternoonAttempt = persistAttempt(user, puzzle, false, 1, -8, 1004);
        PuzzleAttempt ignoredPreviousDayAttempt = persistAttempt(user, puzzle, true, 0, 20, 1024);
        PuzzleAttempt otherUserAttempt = persistAttempt(otherUser, puzzle, true, 0, 10, 1010);
        PuzzleAttempt ignoredAttemptWithoutResultingElo = persistAttempt(otherUser, puzzle, false, 1, -6, null);
        entityManager.flush();

        LocalDateTime startOfDay = LocalDateTime.of(2026, 6, 9, 0, 0);
        updateAttemptDate(userMorningAttempt.getId(), LocalDateTime.of(2026, 6, 9, 9, 0));
        updateAttemptDate(userAfternoonAttempt.getId(), LocalDateTime.of(2026, 6, 9, 15, 0));
        updateAttemptDate(ignoredPreviousDayAttempt.getId(), LocalDateTime.of(2026, 6, 8, 23, 0));
        updateAttemptDate(otherUserAttempt.getId(), LocalDateTime.of(2026, 6, 9, 12, 0));
        updateAttemptDate(ignoredAttemptWithoutResultingElo.getId(), LocalDateTime.of(2026, 6, 9, 13, 0));
        entityManager.clear();

        // When
        List<PuzzleAttemptRepository.UserDailyEloChange> result = puzzleAttemptRepository.findDailyEloChangesSince(startOfDay);

        // Then
        assertThat(result)
                .extracting(
                        PuzzleAttemptRepository.UserDailyEloChange::getFirebaseUid,
                        PuzzleAttemptRepository.UserDailyEloChange::getEloChange
                )
                .containsExactlyInAnyOrder(
                        tuple("user-1", 4L),
                        tuple("user-2", 10L)
                );
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
