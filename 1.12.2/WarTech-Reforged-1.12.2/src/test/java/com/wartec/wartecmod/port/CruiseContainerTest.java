package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.gameplay.TileEntityCruiseFabricator;
import com.wartec.wartecmod.port.gui.ContainerCruiseFabricator;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class CruiseContainerTest {
    @BeforeClass public static void bootstrap() { Bootstrap.register(); }
    @Test public void tenTypedSlotsAndTwoOutputPlanSlotsArePresent() {
        TileEntityCruiseFabricator tile=new TileEntityCruiseFabricator();
        ContainerCruiseFabricator container=new ContainerCruiseFabricator(new InventoryPlayer(null),tile);
        assertEquals(48,container.inventorySlots.size());assertEquals(12,tile.getSizeInventory());
        for(CruisePartDefinition part:CruisePartDefinition.values()) {
            ItemStack stack=new ItemStack(WarTechContent.CRUISE_MODULE,1,part.ordinal());
            for(CruiseSlot slot:CruiseSlot.values()) assertEquals(part.getSlot()==slot,container.getSlot(slot.ordinal()).isItemValid(stack));
            assertFalse(container.getSlot(11).isItemValid(stack));
        }
    }
    @Test public void shiftClickMovesExactlyOnePartToItsTypedSlot() {
        InventoryPlayer inventory=new InventoryPlayer(null);TileEntityCruiseFabricator tile=new TileEntityCruiseFabricator();
        ContainerCruiseFabricator container=new ContainerCruiseFabricator(inventory,tile);
        inventory.setInventorySlotContents(0,new ItemStack(WarTechContent.CRUISE_MODULE,5,CruisePartDefinition.ENGINE_STANDARD.ordinal()));
        assertFalse(container.transferStackInSlot(null,39).isEmpty());
        assertEquals(4,inventory.getStackInSlot(0).getCount());assertEquals(1,tile.getStackInSlot(1).getCount());
        assertSame(CruisePartDefinition.ENGINE_STANDARD,WarTechContent.CRUISE_MODULE.getDefinition(tile.getStackInSlot(1)));
        assertTrue(container.transferStackInSlot(null,39).isEmpty());assertEquals(4,inventory.getStackInSlot(0).getCount());
    }
    @Test public void cruiseTabDoesNotEnablePausedStrategicContent() {
        assertEquals(WarTechCreativeTabs.CUSTOM_CRUISE,WarTechContent.CRUISE_MODULE.getCreativeTab());
        assertEquals(WarTechCreativeTabs.CUSTOM_CRUISE,WarTechContent.ASSEMBLED_CRUISE.getCreativeTab());
        assertFalse(StrategicFeature.isEnabled());
    }
}
