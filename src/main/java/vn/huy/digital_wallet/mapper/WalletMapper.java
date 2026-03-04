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

  /**
   * Map Wallet entity → WalletResponse DTO.
   * Các field cùng tên (balance, currency, status) được map tự động.
   * createdAt nằm ở BaseEntity, MapStruct vẫn tìm thấy qua getter kế thừa.
   */
  @Mapping(source = "id", target = "id")
  @Mapping(source = "balance", target = "balance")
  @Mapping(source = "currency", target = "currency")
  @Mapping(source = "status", target = "status")
  @Mapping(source = "createdAt", target = "createdAt")
  WalletResponse toResponse(Wallet wallet);
}
