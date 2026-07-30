package com.wartec.wartecmod.compat;

/** Integer-backed energy store exposed to the HBM wire network adapter. */
public interface IWirePoweredEntity {
    int wartecGetWirePower();
    void wartecSetWirePower(int power);
    int wartecGetWireCapacity();
}
