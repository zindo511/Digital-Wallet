package vn.huy.digital_wallet.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.huy.digital_wallet.common.Role;
import vn.huy.digital_wallet.common.Status;

import java.util.List;

@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends AuditableEntity {

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM) // áo cho JDBC Driver biết "Ê, đây là Native Enum dưới DB nhé, đừng gửi nó như VARCHAR"
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(name = "full_name")
    private String fullName;

    @Email
    @Column(unique = true)
    private String email;

    @Column(name = "email_notification_enabled")
    private Boolean emailNotificationEnabled = false;

    @Column(name = "phone_number",length = 20)
    @Pattern(
            regexp = "^(0|84)([35789])([0-9]{8})$",
            message = "Số điện thoại không đúng định dạng Việt Nam (ví dụ: 0912345678)"
    )
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private Status status = Status.ACTIVE;

    @OneToOne(mappedBy = "user")
    private Wallet wallet;

    @OneToMany(mappedBy = "user")
    private List<FundingSource> fundingSources;

    @OneToMany(mappedBy = "user")
    private List<Notification> notifications;

    @OneToMany(mappedBy = "user")
    private List<AuditLog> auditLogs;
}
