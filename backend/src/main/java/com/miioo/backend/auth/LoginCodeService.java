package com.miioo.backend.auth;

import com.miioo.backend.common.BizException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginCodeService {
    private static final long CODE_EXPIRE_SECONDS = 5 * 60;
    private static final long RATE_LIMIT_SECONDS = 60;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, CodeRecord> codeStore = new ConcurrentHashMap<>();
    private final Map<String, Long> rateLimitStore = new ConcurrentHashMap<>();

    public SendCodeResult sendCode(String phone) {
        long now = Instant.now().getEpochSecond();
        Long allowAfter = rateLimitStore.get(phone);
        if (allowAfter != null && allowAfter > now) {
            throw new BizException(429, "请求过于频繁，请稍后再试");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        codeStore.put(phone, new CodeRecord(code, now + CODE_EXPIRE_SECONDS));
        rateLimitStore.put(phone, now + RATE_LIMIT_SECONDS);
        return new SendCodeResult(code, RATE_LIMIT_SECONDS);
    }

    public void verifyAndConsume(String phone, String code) {
        long now = Instant.now().getEpochSecond();
        CodeRecord record = codeStore.get(phone);
        if (record == null || record.expireAt <= now || !record.code.equals(code)) {
            throw new BizException(400, "验证码错误或已过期");
        }
        codeStore.remove(phone);
    }

    public record SendCodeResult(String code, long resendAfterSeconds) {}

    private record CodeRecord(String code, long expireAt) {}
}
