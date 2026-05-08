package com.levon.taskplanner.repository;

import com.levon.taskplanner.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    /** Обновляет только fullName — не трогает passwordHash и другие поля. */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.fullName = :fullName, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    void updateFullNameById(@Param("id") Long id, @Param("fullName") String fullName);

    /** Обновляет только passwordHash. */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.passwordHash = :passwordHash, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    void updatePasswordById(@Param("id") Long id, @Param("passwordHash") String passwordHash);
}