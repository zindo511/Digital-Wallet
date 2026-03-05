package vn.huy.digital_wallet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.huy.digital_wallet.model.Transaction;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction,Long> {

    /**
     * Lấy lịch sử giao dịch của 1 ví (cả gửi lẫn nhận), sắp xếp mới nhất trước
     * Dùng cho: GET /api/transactions?page=0&&size=20
     */
    Page<Transaction> findBySourceWallet_IdOrDestinationWallet_IdOrderByCreatedAtDesc(
            Long sourceWalletId,
            Long destinationWalletId,
            Pageable pageable
    );

    // Lấy chi tiết giao dịch của 1 ví
    Optional<Transaction> findByIdAndSourceWallet_IdOrIdAndDestinationWallet_Id(
            Long id, Long sourceWalletId,
            Long id2, Long destinationWalletId
    );
}
