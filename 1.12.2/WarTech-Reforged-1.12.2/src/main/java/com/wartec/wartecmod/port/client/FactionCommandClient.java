package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.network.FactionCommandSnapshot;

public final class FactionCommandClient {
    private static volatile FactionCommandSnapshot snapshot =
            FactionCommandSnapshot.EMPTY;

    private FactionCommandClient() {
    }

    public static void acceptSnapshot(FactionCommandSnapshot value) {
        snapshot = value == null ? FactionCommandSnapshot.EMPTY : value;
    }

    public static FactionCommandSnapshot getSnapshot() {
        return snapshot;
    }
}
