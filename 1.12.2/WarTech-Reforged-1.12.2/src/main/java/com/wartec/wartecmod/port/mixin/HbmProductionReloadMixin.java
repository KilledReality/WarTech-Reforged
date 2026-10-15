package com.wartec.wartecmod.port.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** HBM reloads its machine lists on recipe sync/disconnect; restore the addon's recipes. */
@Mixin(targets="com.hbm.inventory.recipes.loader.SerializableRecipe",remap=false)
public abstract class HbmProductionReloadMixin {
    @Inject(method="initialize",at=@At("RETURN"),remap=false)
    private static void wartec$reloadProduction(CallbackInfo callback){
        com.wartec.wartecmod.port.production.ProductionRecipes.afterHbmReload();
    }
}
