package com.dewple.activity.service;

public record ActivitySummaryResult(
        Long activityId,
        String thumbnailUrl,
        String activityType,
        String clubName,
        String name,
        String categoryName,
        String regionName,
        int participantCount,
        Integer capacity,
        int likeCount,
        int viewCount,
        int commentCount,
        boolean isLiked
) {
}
