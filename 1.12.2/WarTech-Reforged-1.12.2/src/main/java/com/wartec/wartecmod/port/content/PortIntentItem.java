package com.wartec.wartecmod.port.content;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PortIntentItem extends VariantItem implements IntentProvider {
    private final IntentKind intentKind;
    private final ResourceLocation intentId;

    public PortIntentItem(
        String legacyRegistryName,
        CreativeTabs tab,
        int maxStackSize,
        IntentKind intentKind,
        String intentPath,
        String... variantNames
    ) {
        super(legacyRegistryName, tab, maxStackSize, variantNames);
        this.intentKind = intentKind;
        this.intentId = new ResourceLocation(MOD_ID, PortItem.safePath(intentPath));
    }

    @Override
    public IntentKind getIntentKind() {
        return intentKind;
    }

    @Override
    public ResourceLocation getIntentId(ItemStack stack) {
        return intentId;
    }

    @Override
    public int getIntentVariant(ItemStack stack) {
        return getVariant(stack);
    }

    @Override
    public EnumActionResult onItemUse(
        EntityPlayer player,
        World world,
        BlockPos pos,
        EnumHand hand,
        EnumFacing facing,
        float hitX,
        float hitY,
        float hitZ
    ) {
        return EnumActionResult.PASS;
    }
}
