package vn.huy.digital_wallet.event.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.huy.digital_wallet.common.AuditLogAction;
import vn.huy.digital_wallet.common.TransactionType;
import vn.huy.digital_wallet.event.TransactionCompletedEvent;
import vn.huy.digital_wallet.model.AuditLog;
import vn.huy.digital_wallet.repository.AuditLogRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogListener {

    private final AuditLogRepository auditLogRepository;

    @Async("walletExecutor")
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(TransactionCompletedEvent event) {
        try {
            log.info("[AuditLogListener] Đang xử lý - Thread: {}",
                    Thread.currentThread().getName());

            AuditLog auditLog = new AuditLog();
            auditLog.setUser(event.actor());
            auditLog.setAction(mapAction(event.transaction().getType()));
            auditLog.setIpAddress(event.ipAddress());
            auditLog.setDeviceInfo(trimUserAgent(event.userAgent()));

            auditLogRepository.save(auditLog);

            log.info("[AuditLogListener] Lưu audit log thành công cho user: {}",
                    event.actor().getUsername());

        } catch (Exception ex) {
            log.error("[AuditLogListener] Lỗi: {}", ex.getMessage(), ex);
        }
    }

    // Map TransactionType -> AuditLogAction
    private AuditLogAction mapAction(TransactionType type) {
        return switch (type) {
            case TRANSFER -> AuditLogAction.TRANSFER;
            case DEPOSIT ->  AuditLogAction.DEPOSIT;
            case WITHDRAW ->  AuditLogAction.WITHDRAW;
        };
    }

    // Giới hạn độ dài User-Agent để không vượt quá column size trong DB
    private String trimUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        return userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent;
    }
}
