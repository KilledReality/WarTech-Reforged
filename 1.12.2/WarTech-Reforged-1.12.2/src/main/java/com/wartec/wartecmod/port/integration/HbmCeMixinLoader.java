package com.wartec.wartecmod.port.integration;

import java.util.Collections;
import java.util.List;
import zone.rong.mixinbooter.ILateMixinLoader;
import zone.rong.mixinbooter.MixinLoader;

/** Loads the NTM inventory migration before HBM tile classes are transformed. */
@MixinLoader
public final class HbmCeMixinLoader implements ILateMixinLoader {
    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList(
                "mixins.wartecmod.hbm_inventory.json");
    }
}
