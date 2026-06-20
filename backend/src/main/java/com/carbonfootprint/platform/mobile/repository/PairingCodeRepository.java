package com.carbonfootprint.platform.mobile.repository;

import com.carbonfootprint.platform.mobile.model.PairingCode;

import java.util.Optional;

public interface PairingCodeRepository {
    PairingCode save(PairingCode pairingCode);
    Optional<PairingCode> findByCode(String code);
    void deleteByCode(String code);
}
