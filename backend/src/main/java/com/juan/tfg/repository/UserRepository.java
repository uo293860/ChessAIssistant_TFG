package com.juan.tfg.repository;

import com.juan.tfg.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, String> {

    boolean existsByUsername(String username);

    List<User> findAllByOrderByEloRatingDescUsernameAsc();
}
