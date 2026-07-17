package api.hbm.energy;

import api.hbm.energymk2.IEnergyReceiverMK2;

/**
 * Compatibility bridge for old HBM addons compiled against the pre-MK2 energy API.
 *
 * WarTec 1.1.1 implements api.hbm.energy.IEnergyUser, but HBM 1.0.27 only ships
 * api.hbm.energymk2. By extending the current receiver interface, old tile
 * entities become visible to HBM's MK2 power network without changing their
 * compiled classes.
 */
public interface IEnergyUser extends IEnergyReceiverMK2 {
}
