package com.dewple.user.port;

public interface WithdrawalClubPort {

    void transferPresidentRoles(Long userId);

    void removeFromAllClubs(Long userId);
}
