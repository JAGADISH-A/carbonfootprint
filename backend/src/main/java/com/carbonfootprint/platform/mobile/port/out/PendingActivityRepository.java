package com.carbonfootprint.platform.mobile.port.out;

import com.carbonfootprint.platform.mobile.model.PendingActivity;
import com.carbonfootprint.platform.mobile.model.PendingActivityStatus;

import java.util.Optional;

public interface PendingActivityRepository {
    
    PendingActivity save(PendingActivity pendingActivity);
    
    Optional<PendingActivity> findById(String id);
    
    boolean exists(String id);
    
    void updateStatus(String id, PendingActivityStatus status);
    
    void updateFailure(String id, String errorMessage);
    
    PendingActivity upsert(PendingActivity pendingActivity);

    long countPendingByDeviceId(String deviceId);

    java.util.List<PendingActivity> findPendingByDeviceId(String deviceId);
}


