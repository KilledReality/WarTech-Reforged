package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.compat.MissileTrackingService.FactionSnapshot;

/** Latest immutable faction-network snapshot received from the server. */
public final class FactionCommandClient {
    private static volatile FactionSnapshot snapshot = FactionSnapshot.EMPTY;

    private FactionCommandClient() {
    }

    public static void acceptSnapshot(FactionSnapshot next) {
        snapshot = next == null ? FactionSnapshot.EMPTY : next;
    }

    public static FactionSnapshot getSnapshot() {
        return snapshot;
    }
}
