package vn.huy.digital_wallet.dto.response;

import lombok.*;
import vn.huy.digital_wallet.common.WalletStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletResponse {

    private Long id;
    private BigDecimal balance;
    private String currency;
    private WalletStatus status;
    private LocalDateTime createdAt;
}
