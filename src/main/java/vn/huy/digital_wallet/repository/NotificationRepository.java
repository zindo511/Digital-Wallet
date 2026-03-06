package vn.huy.digital_wallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.huy.digital_wallet.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification,Long> {
}
