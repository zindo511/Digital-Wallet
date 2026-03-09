package vn.huy.digital_wallet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.huy.digital_wallet.dto.ApiResponse;
import vn.huy.digital_wallet.dto.response.NotificationResponse;
import vn.huy.digital_wallet.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  /**
   * GET /api/notifications
   * Lấy danh sách thông báo của user (phân trang, mới nhất trước).
   */
  @GetMapping
  public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getMyNotifications(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    Page<NotificationResponse> data = notificationService.getMyNotifications(page, size);
    return ApiResponse.toResponseEntity(HttpStatus.OK, "Lấy danh sách thông báo thành công", data);
  }

  /**
   * GET /api/notifications/unread-count
   * Số thông báo chưa đọc — dùng để hiển thị badge trên UI.
   */
  @GetMapping("/unread-count")
  public ResponseEntity<ApiResponse<Long>> countUnread() {
    long count = notificationService.countUnread();
    return ApiResponse.toResponseEntity(HttpStatus.OK, "Lấy số thông báo chưa đọc thành công", count);
  }

  /**
   * PATCH /api/notifications/{id}/read
   * Đánh dấu 1 thông báo cụ thể đã đọc.
   */
  @PatchMapping("/{id}/read")
  public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
    notificationService.markAsRead(id);
    return ApiResponse.toResponseEntity(HttpStatus.OK, "Đã đánh dấu thông báo là đã đọc", null);
  }

  /**
   * PATCH /api/notifications/read-all
   * Đánh dấu tất cả thông báo đã đọc.
   */
  @PatchMapping("/read-all")
  public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
    notificationService.markAllAsRead();
    return ApiResponse.toResponseEntity(HttpStatus.OK, "Đã đánh dấu tất cả thông báo là đã đọc", null);
  }
}
