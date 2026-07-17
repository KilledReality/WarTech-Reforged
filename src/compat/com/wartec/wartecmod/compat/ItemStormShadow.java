package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.entity.missile.EntityStormShadow;
import com.wartec.wartecmod.items.ItemKalibrMissile;
import net.minecraft.entity.Entity;

public final class ItemStormShadow extends ItemKalibrMissile {
    public ItemStormShadow() {
        super();
        func_77655_b("ItemStormShadow");
    }

    @Override
    public Class<? extends Entity> getMissile() {
        return EntityStormShadow.class;
    }
}
