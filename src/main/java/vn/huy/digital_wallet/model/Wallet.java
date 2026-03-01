package vn.huy.digital_wallet.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.huy.digital_wallet.common.WalletStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Wallet extends AuditableEntity {

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "pin_hash")
    private String pinHash;

    @Column(name = "pin_failed_count")
    private Integer pinFailedCount = 0;

    @Column(name = "pin_locked_until")
    private LocalDateTime pinLockedUntil;

    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private WalletStatus status = WalletStatus.ACTIVE;

    @Version
    private Long version = 0L;
}
