package com.dewple.user.service;

public record StorageUsageResult(
        long usedBytes,
        long totalBytes
) {
}
