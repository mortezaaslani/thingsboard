package org.thingsboard.server.dao.otp;

public interface OtpService {

    void sendOTP(String phoneNumber);

    boolean verifyOTP(String phoneNumber, int otpCode);
}
