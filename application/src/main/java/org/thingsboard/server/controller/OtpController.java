package org.thingsboard.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.otp.OtpService;

@RestController
@RequestMapping("/api/otp")
public class OtpController {

    private final OtpService otpService;

    @Autowired
    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @ApiOperation(value = "Send OTP",
            notes = "Generates and sends an OTP to the provided phone number. Returns a success message if OTP is sent successfully or an error message if the process fails.")
    @PostMapping("/send")
    public ResponseEntity<String> sendOTP(@RequestParam String phoneNumber) {
        try {
            otpService.sendOTP(phoneNumber);
            return ResponseEntity.ok("OTP sent to " + phoneNumber);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send OTP: " + e.getMessage());
        }
    }

    @ApiOperation(value = "Verify OTP",
            notes = "Verifies the provided OTP against the phone number. Returns a success message if the OTP is valid and matches the phone number, or an error message if the OTP is invalid or an exception occurs.")
    @PostMapping("/verify")
    public ResponseEntity<String> verifyOTP(@RequestParam String phoneNumber, @RequestParam int otpCode) {
        try {
            boolean isValid = otpService.verifyOTP(phoneNumber, otpCode);
            if (isValid) {
                return ResponseEntity.ok("OTP verified successfully");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid OTP");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }
}

