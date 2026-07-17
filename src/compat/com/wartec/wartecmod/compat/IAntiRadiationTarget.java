package com.wartec.wartecmod.compat;

/** A radio-emitting vehicle that can be disabled by an anti-radiation warhead. */
public interface IAntiRadiationTarget {
    void wartecDestroyByAntiRadiationMissile();
}
