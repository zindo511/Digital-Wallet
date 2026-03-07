package vn.huy.digital_wallet.event;

import vn.huy.digital_wallet.model.Transaction;
import vn.huy.digital_wallet.model.User;

public record TransactionCompletedEvent(
        Transaction transaction, // giao dịch vừa hoàn thành
        User actor,              // người thực hiện (người gửi)
        String ipAddress,        // IP hấy từ HttpServletRequest
        String userAgent         // Thiết bị lấy từ Header
) {

}

/*
Event là cái phong bì - nhét dữ liệu vào đây và gửi đi qua loa. Listener sẽ mở phong bì ra xài
 */