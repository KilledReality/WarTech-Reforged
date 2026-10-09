package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.content.ContentHooks;
import com.wartec.wartecmod.port.content.DeployableItem;
import com.wartec.wartecmod.port.content.StrategicFeature;
import com.wartec.wartecmod.port.content.VariantItem;
import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.content.WarTechCreativeTabs;
import com.wartec.wartecmod.port.entity.StrategicFlightData;
import com.wartec.wartecmod.port.gui.WarTechGuiHandler;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import org.junit.BeforeClass;
import org.junit.Test;
import net.minecraftforge.registries.IForgeRegistry;
import com.wartec.wartecmod.port.integration.WarTechRecipeRegistration;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.junit.Assert.*;

public class StrategicFeatureDisabledTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void retainedItemsAreHiddenFromCreativeAndSearch() {
        assertFalse(StrategicFeature.isEnabled());
        VariantItem[] items = {WarTechContent.TOPOL_M_TEL,
                WarTechContent.YARS_TEL, WarTechContent.ORESHNIK_TEL,
                WarTechContent.STRATEGIC_MISSILE};
        for (VariantItem item : items) {
            assertTrue(StrategicFeature.isDisabledItem(item));
            assertTrue(WarTechContent.getItems().contains(item));
            assertNull(item.getCreativeTab());
            NonNullList<ItemStack> entries = NonNullList.create();
            item.getSubItems(CreativeTabs.SEARCH, entries);
            item.getSubItems(WarTechCreativeTabs.CRUISE_MISSILES, entries);
            assertTrue(entries.isEmpty());
        }
        assertFalse(StrategicFeature.isDisabledItem(WarTechContent.UAV_MODULE));
        assertFalse(StrategicFeature.isDisabledItem(WarTechContent.ITEM_ISKANDER_MISSILE));
        assertFalse(StrategicFeature.isDisabledItem(WarTechContent.STRATEGIC_BOMB));
    }

    @Test
    public void savedLaunchersCannotDeployOrConsumeAnItem() {
        final boolean[] dispatched = {false};
        ContentHooks.setHandler(context -> {
            dispatched[0] = true;
            return EnumActionResult.SUCCESS;
        });
        try {
            for (DeployableItem item : new DeployableItem[]{WarTechContent.TOPOL_M_TEL,
                    WarTechContent.YARS_TEL, WarTechContent.ORESHNIK_TEL}) {
                ItemStack stack = new ItemStack(item);
                assertEquals(EnumActionResult.FAIL, ContentHooks.dispatch(item, stack,
                        null, null, EnumHand.MAIN_HAND, BlockPos.ORIGIN, EnumFacing.UP));
                assertEquals(1, stack.getCount());
            }
            assertFalse(dispatched[0]);
        } finally {
            ContentHooks.clearHandler();
        }
    }

    @Test
    public void scheduledWarheadsStaySavedAndNeverTouchTheWorld() {
        NBTTagCompound flight = new NBTTagCompound();
        flight.setInteger("System", 2);
        flight.setDouble("TargetX", 120.5D);
        flight.setDouble("TargetZ", -250.5D);
        flight.setLong("Arrival", 1L);
        NBTTagList flights = new NBTTagList();
        flights.appendTag(flight);
        NBTTagCompound saved = new NBTTagCompound();
        saved.setTag("Flights", flights);
        StrategicFlightData data = new StrategicFlightData();
        data.readFromNBT(saved);
        data.tick(null);
        StrategicFlightData.schedule(null, null, 0, 0, 0, null, null, 0, 0);
        NBTTagList retained = data.writeToNBT(new NBTTagCompound()).getTagList("Flights", 10);
        assertEquals(1, retained.tagCount());
        assertEquals(120.5D, retained.getCompoundTagAt(0).getDouble("TargetX"), 0);
        assertEquals(-250.5D, retained.getCompoundTagAt(0).getDouble("TargetZ"), 0);
        assertEquals(1L, retained.getCompoundTagAt(0).getLong("Arrival"));
    }

    @Test
    public void strategicGuiCannotBeOpenedByAnOldPacket() {
        WarTechGuiHandler handler = new WarTechGuiHandler();
        assertNull(handler.getServerGuiElement(WarTechGuiHandler.GUI_STRATEGIC_TEL,
                null, null, 0, 0, 0));
        assertNull(handler.getClientGuiElement(WarTechGuiHandler.GUI_STRATEGIC_TEL,
                null, null, 0, 0, 0));
    }

    @Test
    public void recipeRegistrationSkipsAllSixStrategicOutputs() throws Exception {
        final int[] registrations = {0};
        IForgeRegistry<IRecipe> registry = (IForgeRegistry<IRecipe>) Proxy.newProxyInstance(
                IForgeRegistry.class.getClassLoader(), new Class<?>[]{IForgeRegistry.class},
                (proxy, method, args) -> {
                    if ("register".equals(method.getName())) registrations[0]++;
                    return null;
                });
        Method shaped = WarTechRecipeRegistration.class.getDeclaredMethod("shaped",
                IForgeRegistry.class, String.class, ItemStack.class, Object[].class);
        shaped.setAccessible(true);
        ItemStack[] disabled = {new ItemStack(WarTechContent.TOPOL_M_TEL),
                new ItemStack(WarTechContent.YARS_TEL), new ItemStack(WarTechContent.ORESHNIK_TEL),
                new ItemStack(WarTechContent.STRATEGIC_MISSILE, 1, 0),
                new ItemStack(WarTechContent.STRATEGIC_MISSILE, 1, 1),
                new ItemStack(WarTechContent.STRATEGIC_MISSILE, 1, 2)};
        Object[] recipe = {"I", 'I', Items.IRON_INGOT};
        for (ItemStack output : disabled) {
            shaped.invoke(null, registry, "disabled", output, recipe);
        }
        assertEquals(0, registrations[0]);
        shaped.invoke(null, registry, "uav_module_enabled",
                new ItemStack(WarTechContent.UAV_MODULE), recipe);
        assertEquals(1, registrations[0]);
    }
}
