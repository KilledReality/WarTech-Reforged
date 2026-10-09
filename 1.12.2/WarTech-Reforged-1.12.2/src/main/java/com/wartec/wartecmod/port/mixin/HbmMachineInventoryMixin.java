package com.wartec.wartecmod.port.mixin;

import com.wartec.wartecmod.WarTechReforged;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserves the constructor-defined inventory size when old CE NBT is read. */
@Mixin(targets = "com.hbm.tileentity.TileEntityMachineBase", remap = false)
public abstract class HbmMachineInventoryMixin {
    @Shadow
    public ItemStackHandler inventory;

    @Shadow
    protected abstract void resizeInventory(int size);

    @Unique
    private int wartec$expectedInventorySlots;

    @Inject(method = "func_145839_a", at = @At("HEAD"), remap = false)
    private void wartec$captureInventorySize(
            NBTTagCompound compound, CallbackInfo callback) {
        wartec$expectedInventorySlots = inventory == null
                ? 0 : inventory.getSlots();
    }

    @Inject(method = "func_145839_a", at = @At("RETURN"), remap = false)
    private void wartec$restoreInventorySize(
            NBTTagCompound compound, CallbackInfo callback) {
        if (inventory == null || wartec$expectedInventorySlots <= 0
                || inventory.getSlots() >= wartec$expectedInventorySlots) {
            return;
        }
        int oldSlots = inventory.getSlots();
        resizeInventory(wartec$expectedInventorySlots);
        if (WarTechReforged.logger != null) {
            TileEntity tile = (TileEntity) (Object) this;
            WarTechReforged.logger.info(
                    "Repaired NTM inventory during NBT load: {} at {} "
                            + "from {} to {} slots",
                    tile.getClass().getSimpleName(), tile.getPos(), oldSlots,
                    wartec$expectedInventorySlots);
        }
    }
}
