package com.dewple.user.repository;

import com.dewple.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByDeletedAtNotNullAndDeletedAtBefore(OffsetDateTime before);

    boolean existsByPhone(String phone);

    boolean existsByUserId(String userId);

    boolean existsByEmail(String email);

    Optional<User> findByUserId(String userId);

    Optional<User> findByPhone(String phone);

    boolean existsByEmailAndIdNot(String email, Long id);
}
