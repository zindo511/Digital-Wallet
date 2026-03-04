package vn.huy.digital_wallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.huy.digital_wallet.model.Wallet;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet,Long> {

    Optional<Wallet> findByUser_Username(String username);
}
