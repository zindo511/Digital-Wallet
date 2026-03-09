package vn.huy.digital_wallet.common;

/**
 * @param payload JSON nếu COMPLETED, error message nếu FAILED
 */
public record IdempotencyResult(Status status, String payload) {
    public enum Status {
        NEW,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

    public static IdempotencyResult newRequest() {
        return new IdempotencyResult(Status.NEW, null);
    }

    public static IdempotencyResult inProgress() {
        return new IdempotencyResult(Status.IN_PROGRESS, null);
    }

    public static IdempotencyResult completed(String json) {
        return new IdempotencyResult(Status.COMPLETED, json);
    }

    public static IdempotencyResult failed(String error) {
        return new IdempotencyResult(Status.FAILED, error);
    }
}
