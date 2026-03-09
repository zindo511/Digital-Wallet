package vn.huy.digital_wallet.service.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.huy.digital_wallet.dto.response.NotificationResponse;
import vn.huy.digital_wallet.exception.ResourceNotFoundException;
import vn.huy.digital_wallet.model.Notification;
import vn.huy.digital_wallet.model.Wallet;
import vn.huy.digital_wallet.repository.NotificationRepository;
import vn.huy.digital_wallet.repository.WalletRepository;
import vn.huy.digital_wallet.service.NotificationService;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

  private final NotificationRepository notificationRepository;
  private final WalletRepository walletRepository;

  @Override
  public Page<NotificationResponse> getMyNotifications(int page, int size) {
    Long userId = getCurrentUserId();
    return notificationRepository
        .findByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
        .map(this::toResponse);
  }

  @Override
  public long countUnread() {
    return notificationRepository.countByUser_IdAndIsReadFalse(getCurrentUserId());
  }

  @Override
  @Transactional
  public void markAsRead(Long id) {
    Long userId = getCurrentUserId();
    Notification notification = notificationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo"));

    // Chỉ cho phép đánh dấu thông báo của chính mình
    if (!notification.getUser().getId().equals(userId)) {
      throw new ResourceNotFoundException("Không tìm thấy thông báo");
    }

    notification.setIsRead(true);
    notificationRepository.save(notification);
  }

  @Override
  @Transactional
  public void markAllAsRead() {
    notificationRepository.markAllAsReadByUserId(getCurrentUserId());
  }

  // --- PRIVATE HELPERS ---
  private Long getCurrentUserId() {
    String username = Objects.requireNonNull(
        SecurityContextHolder.getContext().getAuthentication()).getName();
    Wallet wallet = walletRepository.findByUser_Username(username)
        .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
    return wallet.getUser().getId();
  }

  private NotificationResponse toResponse(Notification n) {
    return NotificationResponse.builder()
        .id(n.getId())
        .title(n.getTitle())
        .message(n.getMessage())
        .type(n.getType())
        .isRead(n.getIsRead())
        .transactionId(n.getTransaction() != null ? n.getTransaction().getId() : null)
        .createdAt(n.getCreatedAt())
        .build();
  }
}
