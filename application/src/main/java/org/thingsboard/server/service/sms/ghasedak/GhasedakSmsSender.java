package org.thingsboard.server.service.sms.ghasedak;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.rule.engine.api.sms.exception.SmsException;
import org.thingsboard.rule.engine.api.sms.exception.SmsParseException;
import org.thingsboard.rule.engine.api.sms.exception.SmsSendException;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.service.sms.AbstractSmsSender;

@Slf4j
public class GhasedakSmsSender extends AbstractSmsSender {

    private static final String GHASEDAK_API_URL = "https://api.ghasedak.me/v2/sms/send/simple";
    private static final String GHASEDAK_API_KEY = "9697d0c8420230127cfcb62da673e9b4dd39235c51660f257d02008878700166"; // تنظیم کنید کلید API خود را
    private final RestTemplate restTemplate = new RestTemplate();
    private String validatePhoneNumberGhasedak(String phoneNumber) throws SmsParseException {
        phoneNumber = phoneNumber.trim();
        if (!E_164_PHONE_NUMBER_PATTERN.matcher(phoneNumber).matches()) {
            throw new SmsParseException("Invalid phone number format. Phone number must be in E.164 format.");
        }
        return phoneNumber;
    }

    public GhasedakSmsSender() {

    }

    @Override
    public int sendSms(String numberTo, String message) throws SmsException {
        numberTo = this.validatePhoneNumberGhasedak(numberTo);
        message = "لینک فعال سازی حساب کاربری: " + this.prepareMessage(message);
        try {
            MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
            payload.add("message", message);
            payload.add("receptor", numberTo);
            payload.add("linenumber", "30005006005544");

            // تنظیم headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("apikey", GHASEDAK_API_KEY);
            headers.setCacheControl("no-cache");

            // ایجاد درخواست HTTP
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(payload, headers);

            // ارسال درخواست
            ResponseEntity<String> response = restTemplate.exchange(GHASEDAK_API_URL, HttpMethod.POST, requestEntity, String.class);


            return this.countMessageSegments(message);
        } catch (Exception e) {
            throw new SmsSendException("Failed to send SMS message - " + e.getMessage(), e);
        }
    }

//    @Override
//    public int sendSms(String numberTo, String message) throws SmsException {
//        numberTo = this.validatePhoneNumberGhasedak(numberTo);
//        message = this.prepareMessage(message);
//        try {
//            KavenegarApi api = new KavenegarApi("466E714F6453784338556A4A417A41694E49766C77426D354533793178424D30332B4D42767A79653271493D");
//            api.send("10008663", numberTo, message);
//            return this.countMessageSegments(message);
//        } catch (Exception e) {
//            throw new SmsSendException("Failed to send SMS message - " + e.getMessage(), e);
//        }
//    }

    @Override
    public void destroy() {

    }
}
