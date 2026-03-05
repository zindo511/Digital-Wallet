package vn.huy.digital_wallet.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.huy.digital_wallet.dto.response.TransactionResponse;
import vn.huy.digital_wallet.model.Transaction;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(source = "sourceWallet.id", target = "sourceWalletId")
    @Mapping(source = "destinationWallet.id", target = "destinationWalletId")
    TransactionResponse toResponse(Transaction transaction);
}
