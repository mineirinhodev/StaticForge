package com.ffxiv.planner.auth.repository;

import com.ffxiv.planner.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.oauthProvider = :provider AND u.oauthProviderId = :providerId")
    Optional<User> findByOAuthProvider(@Param("provider") String provider,
                                       @Param("providerId") String providerId);

    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<User> findActiveById(@Param("id") UUID id);
}
