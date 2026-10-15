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

public class StrategicFeatureTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test public void onlyRequestedSystemsAreAvailable() {
        assertTrue(StrategicFeature.isEnabled());
        assertFalse(StrategicFeature.isDisabledItem(WarTechContent.TOPOL_M_TEL));
        assertFalse(StrategicFeature.isDisabledItem(WarTechContent.ORESHNIK_TEL));
        assertFalse(StrategicFeature.isDisabledItem(WarTechContent.ISKANDER_TEL));
        assertTrue(StrategicFeature.isDisabledItem(WarTechContent.YARS_TEL));
        assertNull(WarTechContent.YARS_TEL.getCreativeTab());
        NonNullList<ItemStack> missiles=NonNullList.create();
        WarTechContent.STRATEGIC_MISSILE.getSubItems(CreativeTabs.SEARCH,missiles);
        assertEquals(2,missiles.size());assertEquals(0,missiles.get(0).getMetadata());assertEquals(2,missiles.get(1).getMetadata());
    }
    @Test public void pausedYarsCannotDispatchButRequestedVehiclesCan() {
        final int[] dispatched={0};ContentHooks.setHandler(context->{ dispatched[0]++;return EnumActionResult.SUCCESS; });
        try {
            assertEquals(EnumActionResult.FAIL,ContentHooks.dispatch(WarTechContent.YARS_TEL,new ItemStack(WarTechContent.YARS_TEL),null,null,EnumHand.MAIN_HAND,BlockPos.ORIGIN,EnumFacing.UP));
            for(DeployableItem item:new DeployableItem[]{WarTechContent.TOPOL_M_TEL,WarTechContent.ORESHNIK_TEL,WarTechContent.ISKANDER_TEL})
                assertEquals(EnumActionResult.SUCCESS,ContentHooks.dispatch(item,new ItemStack(item),null,null,EnumHand.MAIN_HAND,BlockPos.ORIGIN,EnumFacing.UP));
            assertEquals(3,dispatched[0]);
        } finally { ContentHooks.clearHandler(); }
    }
    @Test public void savedFlightsPreserveIdentityAcrossRepeatedLoads() {
        NBTTagCompound flight=new NBTTagCompound();flight.setInteger("System",2);flight.setDouble("TargetX",120.5);flight.setDouble("TargetZ",-250.5);flight.setLong("Arrival",1);
        NBTTagList list=new NBTTagList();list.appendTag(flight);NBTTagCompound nbt=new NBTTagCompound();nbt.setTag("Flights",list);
        StrategicFlightData data=new StrategicFlightData();data.readFromNBT(nbt);data.tick(null);
        NBTTagCompound saved=data.writeToNBT(new NBTTagCompound());
        NBTTagCompound first=saved.getTagList("Flights",10).getCompoundTagAt(0);
        assertTrue(first.hasUniqueId("Id"));assertEquals(120.5,first.getDouble("TargetX"),0);
        data.readFromNBT(saved);assertEquals(first.getUniqueId("Id"),data.writeToNBT(new NBTTagCompound()).getTagList("Flights",10).getCompoundTagAt(0).getUniqueId("Id"));
    }
    @Test
    public void productionIncludesRequestedSystemsAndReservesYars() {
        for(String ref:new String[]{"wartecmod:topolmtel@0","wartecmod:oreshniktel@0","wartecmod:strategicmissile@0","wartecmod:strategicmissile@2"})
            assertEquals(ref,"proposed",com.wartec.wartecmod.port.production.ProductionCatalog.recipe(ref).get("status").getAsString());
        assertEquals("reserved",com.wartec.wartecmod.port.production.ProductionCatalog.recipe("wartecmod:yarstel@0").get("status").getAsString());
        assertEquals("reserved",com.wartec.wartecmod.port.production.ProductionCatalog.recipe("wartecmod:strategicmissile@1").get("status").getAsString());
    }
}
