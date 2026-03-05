package vn.huy.digital_wallet.dto.response;

import lombok.*;
import vn.huy.digital_wallet.common.TransactionStatus;
import vn.huy.digital_wallet.common.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private Long id;
    private String transactionReference;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private Long sourceWalletId;
    private Long destinationWalletId;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private BigDecimal fee;
    private LocalDateTime createdAt;
}
