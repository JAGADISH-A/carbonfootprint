package com.carbonfootprint.platform.mobile.repository;

import com.carbonfootprint.platform.mobile.model.Device;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository {
    Device save(Device device);
    Optional<Device> findById(String id);
    Optional<Device> findByDeviceId(String deviceId);
    Optional<Device> findByRefreshTokenHash(String refreshTokenHash);
    List<Device> findByUserId(String userId);
    void deleteById(String id);
}
