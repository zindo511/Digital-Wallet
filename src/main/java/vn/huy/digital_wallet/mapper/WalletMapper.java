package vn.huy.digital_wallet.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.huy.digital_wallet.dto.response.WalletResponse;
import vn.huy.digital_wallet.model.Wallet;

/**
 * MapStruct tự động generate class WalletMapperImpl tại compile time.
 * componentModel = "spring" → inject được bằng @Autowired / constructor
 * injection.
 */
@Mapper(componentModel = "spring")
public interface WalletMapper {

  WalletResponse toResponse(Wallet wallet);
}
