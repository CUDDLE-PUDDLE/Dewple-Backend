package com.dewple.user.repository.custom;

import com.dewple.common.enums.VerificationType;
import com.dewple.user.entity.Verification;

import java.util.Optional;

public interface VerificationRepositoryCustom {

    Optional<Verification> findLatestByDataAndType(String data, VerificationType type);
}
