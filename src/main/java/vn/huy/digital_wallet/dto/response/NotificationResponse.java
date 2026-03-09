package vn.huy.digital_wallet.dto.response;

import lombok.*;
import vn.huy.digital_wallet.common.NotificationType;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

  private Long id;
  private String title;
  private String message;
  private NotificationType type;
  private Boolean isRead;
  private Long transactionId;
  private LocalDateTime createdAt;
}
