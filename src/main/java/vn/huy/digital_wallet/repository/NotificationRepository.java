package vn.huy.digital_wallet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.huy.digital_wallet.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  Page<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

  long countByUser_IdAndIsReadFalse(Long userId);

  @Modifying
  @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
  void markAllAsReadByUserId(@Param("userId") Long userId);
}
