package vn.huy.digital_wallet.event.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.huy.digital_wallet.common.NotificationType;
import vn.huy.digital_wallet.common.TransactionType;
import vn.huy.digital_wallet.event.TransactionCompletedEvent;
import vn.huy.digital_wallet.model.Notification;
import vn.huy.digital_wallet.repository.NotificationRepository;

@Component // Spring quản lý class này như 1 Bean
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final NotificationRepository notificationRepository;

    @Async("walletExecutor") // chạy method trên thread pool "walletExecutor"
                             // (đúng tên bean đã đặt ở AsyncConfig)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    // chỉ kích hoạt sau khi transaction gốc commit xong vào DB
    public void handle(TransactionCompletedEvent event) {
        try {
            // đọc log xem nó có hoạt động không
            log.info("[NotificationListener] Đang xử lý - Thread: {}",
                    Thread.currentThread().getName());

            // --- Notification cho người gửi ---
            Notification senderNoti = new Notification();
            senderNoti.setUser(event.actor());
            senderNoti.setTransaction(event.transaction());
            senderNoti.setTitle(buildTitle(event.transaction().getType()));
            senderNoti.setMessage(buildSenderMessage(event));
            senderNoti.setType(NotificationType.TRANSFER);
            senderNoti.setIsRead(false);
            notificationRepository.save(senderNoti);

            // --- Notification cho người Nhận (chỉ noti khi chuyển tiền)
            if (event.transaction().getType() == TransactionType.TRANSFER
                    && event.transaction().getDestinationWallet() != null) {

                Notification receiverNoti = getNotification(event);
                notificationRepository.save(receiverNoti);
            }

            log.info("[NotificationListener] Lưu notification thành công cho txn: {}",
                    event.transaction().getTransactionReference());
        } catch (Exception ex) {
            // Async thread bị lỗi -> exception biến mất im lặng nếu không catch
            log.error("[NotificationListener] Lỗi: {}", ex.getMessage(), ex);
        }
    }

    private static Notification getNotification(TransactionCompletedEvent event) {
        Notification receiverNoti = new Notification();
        receiverNoti.setUser(event.transaction().getDestinationWallet().getUser());
        receiverNoti.setTransaction(event.transaction());
        receiverNoti.setTitle("Nhận tiền thành công");
        receiverNoti.setMessage("Bạn vừa nhận được "
                + event.transaction().getAmount()
                + "đ từ " + event.actor().getUsername());
        receiverNoti.setType(NotificationType.TRANSFER);
        receiverNoti.setIsRead(false);
        return receiverNoti;
    }

    private String buildTitle(TransactionType type) {
        return switch (type) {
            case TRANSFER -> "Chuyển tiền thành công";
            case DEPOSIT -> "Nạp tiền thành công";
            case WITHDRAW -> "Rút tiền thành công";
        };
    }

    private String buildSenderMessage(TransactionCompletedEvent event) {
        return switch (event.transaction().getType()) {
            case TRANSFER -> "Bạn vừa chuyển "
                    + event.transaction().getAmount() + "đ"
                    + " đến ví #" + event.transaction().getDestinationWallet().getId();
            case DEPOSIT -> "Bạn vừa nạp "
                    + event.transaction().getAmount() + " đ vào ví";
            case WITHDRAW -> "Bạn vừa rút "
                    + event.transaction().getAmount() + " đ vào ví";
        };
    }
}
