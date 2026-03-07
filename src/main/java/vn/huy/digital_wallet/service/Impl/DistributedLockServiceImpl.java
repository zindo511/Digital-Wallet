package vn.huy.digital_wallet.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import vn.huy.digital_wallet.service.DistributedLockService;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedLockServiceImpl implements DistributedLockService {

    private static final String PREFIX = "wallet:lock";
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean tryLock(Long walletId, String lockValue) {
        String key = PREFIX + walletId;

        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, lockValue, LOCK_TTL);
        boolean success = Boolean.TRUE.equals(acquired);

        // bắn ra log để sau này debug
        if (success) {
            log.debug("Lock acquired: key={}", key);
        } else {
            log.warn("Lock failed: key={}, ttl={}", key, LOCK_TTL);
        }
        return success;
    }

    @Override
    public void releaseLock(Long walletId, String lockValue) {
        String key = PREFIX + walletId;
        String stored = stringRedisTemplate.opsForValue().get(key);

        if (lockValue.equals(stored)) {
            stringRedisTemplate.delete(key);
            log.debug("Lock released: key={}", key);
        } else {
            log.warn("Không xoá lock vì lockValue không khớp: key={}, stored={}", key, stored);
        }
    }
}
