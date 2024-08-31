package org.thingsboard.server.dao.otp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class OtpServiceImpl implements OtpService {

    private static final String API_URL = "https://api.ghasedak.me/v2/verification/send/simple";
    private static final String API_KEY = "9697d0c8420230127cfcb62da673e9b4dd39235c51660f257d02008878700166";


    private final RestTemplate restTemplate = new RestTemplate();

    private Map<String, OTPData> otpStorage = new HashMap<>();

    @Autowired
    private OtpRequestLimiterImpl otpRequestLimiter;

    @Override
    public void sendOTP(String phoneNumber) {
        // بررسی می‌کنیم که آیا شماره تلفن بن شده است یا خیر
        if (otpRequestLimiter.isBanned(phoneNumber)) {
            throw new IllegalStateException("This phone number is temporarily blocked due to too many requests.");
        }

        // چک کردن اینکه آیا کد هنوز معتبر است یا خیر
        if (otpStorage.containsKey(phoneNumber) && otpStorage.get(phoneNumber).getExpiry() > System.currentTimeMillis()) {
            throw new IllegalStateException("An OTP has already been sent. Please try again after the current OTP expires.");
        }

        Random random = new Random();
        int otpCode = 100000 + random.nextInt(900000);
        long otpExpiry = System.currentTimeMillis() + 2 * 60 * 1000; // OTP معتبر به مدت 2 دقیقه

        otpStorage.put(phoneNumber, new OTPData(otpCode, otpExpiry));

        String message = String.valueOf(otpCode);

        boolean success = sendSMS(phoneNumber, message);

        if (!success) {
            throw new RuntimeException("Failed to send OTP");
        }

        // ثبت درخواست برای این شماره تلفن
        otpRequestLimiter.logRequest(phoneNumber);
    }

    @Override
    public boolean verifyOTP(String phoneNumber, int otpCode) {
        OTPData otpData = otpStorage.get(phoneNumber);
        if (otpData == null) {
            throw new IllegalArgumentException("Invalid phone number");
        }

        if (otpData.getExpiry() < System.currentTimeMillis()) {
            throw new IllegalArgumentException("OTP has expired");
        }

        if (otpData.getCode() != otpCode) {
            throw new IllegalArgumentException("Invalid OTP code");
        }
        //حذف otp پس از تایید
        otpStorage.remove(phoneNumber);
        return true; // OTP صحیح است
    }

    public boolean sendSMS(String phoneNumber, String message) {
        try {
            // تنظیم هدرها
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/x-www-form-urlencoded");
            headers.set("apikey", API_KEY);

            // ساختن بدنه درخواست
            String payload = String.format("receptor=%s&template=iot&type=1&param1=%s", phoneNumber, message);

            HttpEntity<String> entity = new HttpEntity<>(payload, headers);

            // ارسال درخواست
            ResponseEntity<String> response = restTemplate.exchange(API_URL, HttpMethod.POST, entity, String.class);

            // بررسی وضعیت پاسخ
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("SMS sent successfully: {}", response.getBody());
                return true;
            } else {
                log.error("Failed to send SMS: {}", response.getBody());
                return false;
            }
        } catch (Exception e) {
            log.error("Error sending SMS: {}", e.getMessage());
            return false;
        }
    }

    //    private boolean sendSMS(String phoneNumber, String message) {
//        try {
//            KavenegarApi api = new KavenegarApi(API_KEY);
//            api.send(SENDER_PHONE_NUMBER, phoneNumber, message);
//            return true;
//        } catch (Exception e) {
//            System.err.println("Error sending SMS: " + e.getMessage());
//            return false;
//        }
//    }

    @Scheduled(fixedRate = 3600000) // اجرا هر 1 ساعت برای پاکسازی کدهای منقضی شده
    public void removeExpiredOtps() {
        long currentTime = System.currentTimeMillis();
        otpStorage.entrySet().removeIf(entry -> entry.getValue().getExpiry() < currentTime);
    }

    private static class OTPData {
        private int code;
        private long expiry;

        public OTPData(int code, long expiry) {
            this.code = code;
            this.expiry = expiry;
        }

        public int getCode() {
            return code;
        }

        public long getExpiry() {
            return expiry;
        }
    }
}

