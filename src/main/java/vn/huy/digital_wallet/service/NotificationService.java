package vn.huy.digital_wallet.service;

import org.springframework.data.domain.Page;
import vn.huy.digital_wallet.dto.response.NotificationResponse;

public interface NotificationService {

  /** Lấy danh sách thông báo của user hiện tại (phân trang) */
  Page<NotificationResponse> getMyNotifications(int page, int size);

  /** Số thông báo chưa đọc */
  long countUnread();

  /** Đánh dấu 1 thông báo đã đọc */
  void markAsRead(Long id);

  /** Đánh dấu tất cả thông báo đã đọc */
  void markAllAsRead();
}
