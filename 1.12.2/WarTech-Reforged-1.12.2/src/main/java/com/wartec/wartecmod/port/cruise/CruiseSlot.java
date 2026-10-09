package com.wartec.wartecmod.port.cruise;

public enum CruiseSlot {
    BODY, ENGINE, FUEL, WINGS, NAVIGATION, SEEKER, WARHEAD, FUSE, LAUNCH, LINK;
    public static CruiseSlot byIndex(int index) {
        return index >= 0 && index < values().length ? values()[index] : null;
    }
}
