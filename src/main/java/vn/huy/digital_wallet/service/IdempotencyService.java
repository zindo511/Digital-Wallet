package vn.huy.digital_wallet.service;

public interface IdempotencyService {

    /**
     * Kiểm tra key đã được xử lý chưa
     * Nếu rồi → throw DuplicationRequestException
     * Nếu chưa → đánh dấu "đang xử lý" vào Redis
     * @param key Idempotency key từ header X-Idempotency-key
     */
    void checkAndMark(String key);

    void saveResult(String key, String result);

    String getResult(String key);
}
