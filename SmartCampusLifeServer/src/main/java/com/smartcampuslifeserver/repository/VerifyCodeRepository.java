package com.smartcampuslifeserver.repository;

import com.smartcampuslifeserver.entity.VerifyCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VerifyCodeRepository extends JpaRepository<VerifyCode, VerifyCode.VerifyCodeId> {

    List<VerifyCode> findByPhone(String phone);

    Optional<VerifyCode> findByPhoneAndType(String phone, String type);

    void deleteByExpireTimeBefore(LocalDateTime time);
}
