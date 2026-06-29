package com.juan.tfg.repository;

import com.juan.tfg.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Checks whether a username is already used by any user.
     *
     * @param username the username to check.
     * @return true when the username exists.
     */
    boolean existsByUsername(String username);

    /**
     * Checks whether a username is used by a user other than the provided Firebase UID.
     *
     * @param username the username to check.
     * @param firebaseUid the Firebase UID to exclude.
     * @return true when another user already owns the username.
     */
    boolean existsByUsernameAndFirebaseUidNot(String username, String firebaseUid);

    /**
     * Finds all users ordered for leaderboard display.
     *
     * @return users ordered by descending Elo rating and ascending username.
     */
    List<User> findAllByOrderByEloRatingDescUsernameAsc();
}
