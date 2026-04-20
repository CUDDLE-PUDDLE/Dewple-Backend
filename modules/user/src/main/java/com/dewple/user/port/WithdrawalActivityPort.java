package com.dewple.user.port;

public interface WithdrawalActivityPort {

    void cancelConfirmedParticipations(Long userId);

    void transferOrCancelLeaderActivities(Long userId);
}
