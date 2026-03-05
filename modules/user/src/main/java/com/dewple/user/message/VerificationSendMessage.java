package com.dewple.user.message;

import com.dewple.common.enums.VerificationType;

public record VerificationSendMessage(
        VerificationType type,
        String target,
        String code
) {
}
