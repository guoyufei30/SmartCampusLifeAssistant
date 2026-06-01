package com.smartcampuslifeserver.repository;

import com.smartcampuslifeserver.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserId(String userId);

    Optional<User> findByPhone(String phone);

    Page<User> findByStatus(String status, Pageable pageable);

    Page<User> findByNicknameContainingOrPhoneContainingOrUserIdContaining(
        String nickname, String phone, String userId, Pageable pageable);

    Page<User> findByNicknameContainingOrPhoneContainingOrUserIdContainingAndStatus(
        String nickname, String phone, String userId, String status, Pageable pageable);

    long countByRole(String role);

    long countByStatus(String status);
}
