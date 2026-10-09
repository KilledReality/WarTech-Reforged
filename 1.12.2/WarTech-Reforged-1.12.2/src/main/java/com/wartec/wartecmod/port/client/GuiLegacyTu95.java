package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import net.minecraft.entity.player.InventoryPlayer;

/** Tu-95 keeps its command IDs, using the same bounded layout as other carriers. */
public final class GuiLegacyTu95 extends GuiLegacyAircraft {
    public GuiLegacyTu95(InventoryPlayer inventory,EntityWarTechBase bomber) { super(inventory,bomber); }
}
