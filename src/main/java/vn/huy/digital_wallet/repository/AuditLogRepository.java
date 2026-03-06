package vn.huy.digital_wallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.huy.digital_wallet.model.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog,Long> {
}
