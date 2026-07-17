package com.wartec.wartecmod.compat;

public interface VlsInterceptor {
    void wartecSetTarget(int entityId);

    int wartecGetTarget();

    int wartecGetTier();
}
