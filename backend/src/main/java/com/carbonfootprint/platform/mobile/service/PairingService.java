package com.carbonfootprint.platform.mobile.service;

import com.carbonfootprint.platform.mobile.dto.DeviceRegistrationRequest;
import com.carbonfootprint.platform.mobile.dto.PairingCodeResponse;
import com.carbonfootprint.platform.mobile.dto.TokenResponse;
import com.carbonfootprint.platform.mobile.model.Device;
import com.carbonfootprint.platform.mobile.model.PairingCode;
import com.carbonfootprint.platform.mobile.repository.DeviceRepository;
import com.carbonfootprint.platform.mobile.repository.PairingCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PairingService {

    private final PairingCodeRepository pairingCodeRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceTokenService deviceTokenService;
    
    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Removed ambiguous chars like I, O, 1, 0
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 8;
    private static final int PAIRING_CODE_VALIDITY_MINUTES = 5;
    private static final int REFRESH_TOKEN_VALIDITY_DAYS = 90;

    /**
     * Generates a new short-lived pairing code for an authenticated web user.
     */
    public PairingCodeResponse generatePairingCode(String userId) {
        String code = generateRandomCode();
        
        // Add a dash in the middle for readability (e.g. 6BHF-92KL)
        String formattedCode = code.substring(0, 4) + "-" + code.substring(4);
        
        Instant expiresAt = Instant.now().plus(PAIRING_CODE_VALIDITY_MINUTES, ChronoUnit.MINUTES);
        
        PairingCode pairingCode = PairingCode.builder()
                .code(formattedCode)
                .userId(userId)
                .expiresAt(expiresAt)
                .build();
                
        pairingCodeRepository.save(pairingCode);
        log.info("Generated pairing code for user: {}", userId);
        
        return PairingCodeResponse.builder()
                .pairingCode(formattedCode)
                .expiresAt(expiresAt)
                .expiresInSeconds(PAIRING_CODE_VALIDITY_MINUTES * 60)
                .build();
    }

    /**
     * Completes the pairing flow for a device by validating the code.
     */
    public TokenResponse pairDevice(DeviceRegistrationRequest request) {
        String code = request.getPairingCode();
        
        PairingCode pairingCode = pairingCodeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid pairing code"));
                
        if (pairingCode.isExpired()) {
            pairingCodeRepository.deleteByCode(code);
            throw new IllegalArgumentException("Pairing code has expired");
        }
        
        String userId = pairingCode.getUserId();
        
        // Consume the code so it cannot be reused
        pairingCodeRepository.deleteByCode(code);
        
        // Register or update the device
        String refreshToken = UUID.randomUUID().toString();
        Instant refreshTokenExpiry = Instant.now().plus(REFRESH_TOKEN_VALIDITY_DAYS, ChronoUnit.DAYS);
        
        // Check if device already registered for this ID to update, else create new
        Device device = deviceRepository.findByDeviceId(request.getDeviceId()).orElseGet(() -> 
                Device.builder()
                        .id(UUID.randomUUID().toString())
                        .deviceId(request.getDeviceId())
                        .createdAt(Instant.now())
                        .build());
                        
        device.setUserId(userId);
        device.setDeviceName(request.getDeviceName());
        device.setManufacturer(request.getManufacturer());
        device.setModel(request.getModel());
        device.setAndroidVersion(request.getAndroidVersion());
        device.setAppVersion(request.getAppVersion());
        device.setRefreshTokenHash(refreshToken); // Note: in prod this should be hashed
        device.setRefreshTokenExpiry(refreshTokenExpiry);
        device.setLastSeenAt(Instant.now());
        
        deviceRepository.save(device);
        log.info("Device {} paired successfully to user {}", request.getDeviceId(), userId);
        
        String deviceToken = deviceTokenService.generateDeviceToken(request.getDeviceId(), userId);
        
        return TokenResponse.builder()
                .deviceToken(deviceToken)
                .refreshToken(refreshToken)
                .expiresInSeconds(DeviceTokenService.TOKEN_EXPIRY_SECONDS)
                .build();
    }

    /**
     * Refreshes a device token using a valid refresh token.
     */
    public TokenResponse refreshDeviceToken(String refreshToken) {
        Device device = deviceRepository.findByRefreshTokenHash(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
                
        if (Instant.now().isAfter(device.getRefreshTokenExpiry())) {
            // Revoke the token if expired to be safe
            device.setRefreshTokenHash(null);
            deviceRepository.save(device);
            throw new IllegalArgumentException("Refresh token has expired");
        }
        
        // Generate a new short-lived device token
        String newDeviceToken = deviceTokenService.generateDeviceToken(device.getDeviceId(), device.getUserId());
        
        // Optional: rotate refresh token here as well. For now, keep the same until expiry.
        return TokenResponse.builder()
                .deviceToken(newDeviceToken)
                .refreshToken(refreshToken)
                .expiresInSeconds(DeviceTokenService.TOKEN_EXPIRY_SECONDS)
                .build();
    }
    
    /**
     * Invalidates a device's tokens for logout.
     */
    public void logoutDevice(String deviceId) {
        deviceRepository.findByDeviceId(deviceId).ifPresent(device -> {
            device.setRefreshTokenHash(null);
            deviceRepository.save(device);
            log.info("Device {} logged out successfully", deviceId);
        });
    }

    /**
     * Gets all devices for a given user.
     */
    public java.util.List<Device> getDevicesForUser(String userId) {
        return deviceRepository.findByUserId(userId);
    }

    /**
     * Removes a device for a user.
     */
    public void removeDeviceForUser(String deviceId, String userId) {
        deviceRepository.findByDeviceId(deviceId).ifPresent(device -> {
            if (device.getUserId().equals(userId)) {
                deviceRepository.deleteById(device.getId());
                log.info("Device {} removed by user {}", deviceId, userId);
            } else {
                throw new SecurityException("Device does not belong to user");
            }
        });
    }

    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}
