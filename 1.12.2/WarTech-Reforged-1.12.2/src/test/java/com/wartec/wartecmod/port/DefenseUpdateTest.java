package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.integration.StrikeBlastSafety;
import java.util.UUID;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class DefenseUpdateTest {
    @BeforeClass public static void bootstrap() { Bootstrap.register(); }
    @Test public void conflictingTeamsOverrideSharedOriginalOwner() {
        UUID owner=new UUID(72,1);
        assertFalse(StrikeBlastSafety.allied(owner,"blue",owner,"red"));
        assertTrue(StrikeBlastSafety.allied(owner,"blue",owner,"blue"));
        assertTrue(StrikeBlastSafety.allied(owner,"",owner,""));
    }
    @Test public void allInterceptorsAreInDefenseNotStrikeTab() {
        for(net.minecraft.item.Item item:WarTechContent.getItems()) if(item instanceof MissileItem) {
            MissileProfile profile=((MissileItem)item).getProfile();
            if(profile.getFlightClass()==MissileProfile.FlightClass.INTERCEPTOR || profile==MissileProfile.ASAT)
                assertSame(item.getRegistryName().toString(),WarTechCreativeTabs.AIR_DEFENSE,item.getCreativeTab());
        }
    }
    @Test public void controllersAndMaintenanceAreTogether() {
        assertSame(WarTechCreativeTabs.GEAR,WarTechContent.WARTEC_SALVAGE_WRENCH.getCreativeTab());
        assertSame(WarTechCreativeTabs.GEAR,WarTechContent.WARTECH_IFF_CONFIGURATOR.getCreativeTab());
    }
    @Test public void defenseMachinesBeforeLaunchersBeforeAmmunition() {
        assertTrue(WarTechCreativeTabs.group(new ItemStack(WarTechContent.MOBILE_AIR_DEFENSE_SYSTEM))
            <WarTechCreativeTabs.group(new ItemStack(WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_1)));
    }
}
