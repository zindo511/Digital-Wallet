package vn.huy.digital_wallet.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import vn.huy.digital_wallet.exception.DuplicateRequestException;
import vn.huy.digital_wallet.service.IdempotencyService;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private static final String PREFIX = "idempotency:";
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void checkAndMark(String key) {
        String redisKey = PREFIX + key;
        Boolean isNew = stringRedisTemplate.opsForValue()
                .setIfAbsent(redisKey, IN_PROGRESS, TTL);

        if (!Boolean.TRUE.equals(isNew)) { // phân loại
            String existing = stringRedisTemplate.opsForValue().get(redisKey);
            // đang chờ
            if (IN_PROGRESS.equals(existing)) {
                throw new DuplicateRequestException("Giao dịch vẫn đang được xử lý, vui lòng chờ");
            }
            // đã có kết quả
            throw new DuplicateRequestException("Giao dịch này đã được thực hiện trước đó");
        }
        log.debug("Idempotency key đã được đánh dấu: {}", redisKey);
    }

    @Override
    public void saveResult(String key, String result) {
        String redisKey = PREFIX + key;
        stringRedisTemplate.opsForValue().set(redisKey, result, TTL);
        log.debug("Đã lưu kết quả idempotency cho key: {}", redisKey);
    }

    @Override
    public String getResult(String key) {
        return stringRedisTemplate.opsForValue().get(PREFIX + key);
    }
}
