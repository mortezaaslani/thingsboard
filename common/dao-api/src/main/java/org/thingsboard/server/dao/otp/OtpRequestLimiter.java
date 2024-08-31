package org.thingsboard.server.dao.otp;

public interface OtpRequestLimiter {
    boolean isBanned(String phoneNumber);
    void logRequest(String phoneNumber);
}

