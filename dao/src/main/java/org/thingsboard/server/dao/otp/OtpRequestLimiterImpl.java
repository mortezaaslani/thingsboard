package org.thingsboard.server.dao.otp;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OtpRequestLimiterImpl implements OtpRequestLimiter {

    private static final long THRESHOLD_TIME = 3 * 60 * 1000; // 3 دقیقه
    private static final int MAX_REQUESTS = 4;
    private static final long BAN_DURATION = 10 * 60 * 1000; // 10 دقیقه

    private ConcurrentHashMap<String, List<Long>> requestLog = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Long> bannedNumbers = new ConcurrentHashMap<>();

    @Override
    public boolean isBanned(String phoneNumber) {
        Long banTimestamp = bannedNumbers.get(phoneNumber);
        if (banTimestamp != null) {
            if (System.currentTimeMillis() - banTimestamp < BAN_DURATION) {
                return true;
            } else {
                bannedNumbers.remove(phoneNumber);
            }
        }
        return false;
    }

    @Override
    public void logRequest(String phoneNumber) {
        requestLog.putIfAbsent(phoneNumber, new CopyOnWriteArrayList<>());
        List<Long> timestamps = requestLog.get(phoneNumber);
        timestamps.add(System.currentTimeMillis());

        timestamps.removeIf(time -> System.currentTimeMillis() - time > THRESHOLD_TIME);

        if (timestamps.size() > MAX_REQUESTS) {
            bannedNumbers.put(phoneNumber, System.currentTimeMillis());
        }
    }

}

