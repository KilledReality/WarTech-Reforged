package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.network.UavFleetSnapshot;

public final class UavFleetClient {
    private static volatile UavFleetSnapshot snapshot = UavFleetSnapshot.EMPTY;

    private UavFleetClient() {
    }

    public static void accept(UavFleetSnapshot value) {
        snapshot = value == null ? UavFleetSnapshot.EMPTY : value;
    }

    public static UavFleetSnapshot get() {
        return snapshot;
    }
}
