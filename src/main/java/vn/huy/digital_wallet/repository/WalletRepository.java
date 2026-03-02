package vn.huy.digital_wallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.huy.digital_wallet.model.Wallet;

public interface WalletRepository extends JpaRepository<Wallet,Long> {
}
