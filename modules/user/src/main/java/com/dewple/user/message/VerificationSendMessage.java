package com.dewple.user.message;

import com.dewple.common.enums.VerificationType;

public record VerificationSendMessage(
        Kind kind,
        VerificationType type,
        String target,
        String payload
) {
    public enum Kind {
        VERIFICATION_CODE,
        TEMPORARY_PASSWORD
    }

    public static VerificationSendMessage verificationCode(VerificationType type, String target, String code) {
        return new VerificationSendMessage(Kind.VERIFICATION_CODE, type, target, code);
    }

    public static VerificationSendMessage temporaryPassword(String phone, String temporaryPassword) {
        return new VerificationSendMessage(Kind.TEMPORARY_PASSWORD, VerificationType.PHONE, phone, temporaryPassword);
    }
}
