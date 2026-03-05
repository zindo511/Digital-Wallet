package vn.huy.digital_wallet.service;

public interface DistributedLockService {

    /**
     * Cố gắng lấy lock cho 1 ví
     * @param walletId ID ví cần lock
     * @param lockValue UUID định danh chủ sỡ hữu lock
     * @return true nếu lấy được lock, false nếu đang bị giữ
     */
    boolean tryLock(Long walletId, String lockValue);

    /**
     * Giải phóng lock - chỉ xoá nếu lockValue khớp
     * @param walletId ID ví cần unlock
     * @param lockValue UUID đã dùng khi tryLock
     */
    void releaseLock(Long walletId, String lockValue);
}
