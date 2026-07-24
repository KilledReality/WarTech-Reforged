package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.entity.vehicle.EntityMobileArtillery;
import com.wartec.wartecmod.entity.vehicle.BlockMobileTurretProxy;
import com.wartec.wartecmod.entity.vehicle.MobileTileArty;
import com.wartec.wartecmod.entity.vehicle.MobileTileHimars;
import com.wartec.wartecmod.wartecmod;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

public final class MobileArtilleryContent {
    public static Item mobileArtillery;
    public static Block mobileTurretProxy;

    private MobileArtilleryContent() {
    }

    public static void register() {
        if (mobileArtillery != null) {
            return;
        }
        mobileTurretProxy = new BlockMobileTurretProxy();
        GameRegistry.registerBlock(mobileTurretProxy, "MobileTurretProxy");
        mobileArtillery = new ItemMobileArtillery()
                .func_77637_a(ReforgedCreativeTabs.SUPPORT)
                .func_111206_d("wartecmod:mobile_artillery");
        GameRegistry.registerItem(mobileArtillery, "MobileArtillery");
        GameRegistry.registerTileEntity(MobileTileArty.class, "wartecMobileGreg");
        GameRegistry.registerTileEntity(MobileTileHimars.class, "wartecMobileHenry");
        EntityRegistry.registerModEntity(EntityMobileArtillery.class, "entity_Mobile_Artillery", 30,
                wartecmod.instance, 256, 1, true);
    }
}
