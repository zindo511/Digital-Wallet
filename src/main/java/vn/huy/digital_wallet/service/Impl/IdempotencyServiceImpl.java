package vn.huy.digital_wallet.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import vn.huy.digital_wallet.common.IdempotencyResult;
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
    public IdempotencyResult checkAndMark(String key) {
        String redisKey = PREFIX + key;

        Boolean isNew = stringRedisTemplate.opsForValue()
                .setIfAbsent(redisKey, IN_PROGRESS, TTL);
        if (Boolean.TRUE.equals(isNew)) {
            return IdempotencyResult.newRequest(); // Key mới → xử lý tiếp
        }

        // Key đã tồn tại — đọc để biết đang ở trạng thái nào
        String existing = stringRedisTemplate.opsForValue().get(redisKey);

        if (IN_PROGRESS.equals(existing)) {
            return IdempotencyResult.inProgress();
        }
        if (existing != null && existing.startsWith("COMPLETED:")) {
            return IdempotencyResult.completed(existing.substring("COMPLETED:".length()));
        }
        if (existing != null && existing.startsWith("FAILED:")) {
            return IdempotencyResult.failed(existing.substring("FAILED:".length()));
        }

        return IdempotencyResult.inProgress();
    }

    @Override
    public void saveResult(String key, String result) {
        stringRedisTemplate.opsForValue().set(PREFIX + key, result, TTL);
    }
}
