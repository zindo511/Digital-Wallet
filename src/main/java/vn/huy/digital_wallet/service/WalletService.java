package vn.huy.digital_wallet.service;

import vn.huy.digital_wallet.dto.request.ChangePinRequest;
import vn.huy.digital_wallet.dto.request.SetPinRequest;
import vn.huy.digital_wallet.dto.response.WalletResponse;

public interface WalletService {

    WalletResponse getInfo();

    void setPin(SetPinRequest request);

    void changePin(ChangePinRequest request);
}
