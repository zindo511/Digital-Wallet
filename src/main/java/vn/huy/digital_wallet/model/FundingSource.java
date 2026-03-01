package vn.huy.digital_wallet.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.huy.digital_wallet.common.FundingSourceStatus;
import vn.huy.digital_wallet.common.FundingSourceType;

import java.util.List;

@Entity
@Table(name = "funding_source")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FundingSource extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "bank_code")
    private String bankCode;

    @Column(name = "masked_card_number")
    private String maskedCardNumber;

    private String token;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private FundingSourceType type;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private FundingSourceStatus status;

    @OneToMany(mappedBy = "fundingSource")
    private List<Transaction> transactions;
}
