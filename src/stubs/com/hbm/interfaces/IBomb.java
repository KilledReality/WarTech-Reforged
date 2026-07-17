package com.hbm.interfaces;

public interface IBomb {
    enum BombReturnCode {
        UNDEFINED, DETONATED, TRIGGERED, LAUNCHED, ERROR_MISSING_COMPONENT, ERROR_INCOMPATIBLE, ERROR_NO_BOMB
    }
}
