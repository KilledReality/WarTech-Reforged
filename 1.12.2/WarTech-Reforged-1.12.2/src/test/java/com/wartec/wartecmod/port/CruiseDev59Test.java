package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.uav.*;
import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.entity.EntityCustomUav;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import java.lang.reflect.Method;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/** Screenshot regressions: clean bodies, compact mounting, no terminal waypoint braking. */
public class CruiseDev59Test {
    @BeforeClass public static void bootstrap() {
        Bootstrap.register();
        // NBT round-trip must not depend on which test class registered the item first.
        if(net.minecraft.item.Item.REGISTRY.getNameForObject(WarTechContent.ASSEMBLED_CRUISE)==null)
            net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS.register(WarTechContent.ASSEMBLED_CRUISE);
    }
    @Test public void alignedImpactKeepsFullSpeedEvenCentimetresFromTarget() {
        for(double distance:new double[]{120,24,5,1,.1,.001}) {
            Vec3d delta=new Vec3d(0,-distance,distance);
            assertEquals(1.5,CruiseTerminalApproach.speedLimit(1.5,delta,0,45,1),1e-9);
        }
    }
    @Test public void largeHeadingErrorStillCannotCauseHovering() {
        for(float yaw:new float[]{0,45,90,180}) for(float pitch:new float[]{-65,0,45,90})
            assertTrue(CruiseTerminalApproach.speedLimit(1.5,new Vec3d(0,-.1,.1),yaw,pitch,1)>=.825-1e-9);
    }
    @Test public void impactRunReachesContactAtCruiseSpeedInsteadOfStopping() {
        for(CruisePartDefinition body:CruiseAirframes.bodies()) {
            CruiseBuild b=CruiseBuild.starter(body);double maximum=b.calculateStats().getSpeed();
            Vec3d target=new Vec3d(0,4,0),position=new Vec3d(0,14,-10);int ticks=0;
            while(position.y>4 && ticks++<200) {
                double speed=CruiseTerminalApproach.speedLimit(maximum,target.subtract(position),0,45,b.calculateStats().getTurnRate());
                assertEquals(maximum,speed,1e-7);
                Vec3d next=position.add(CruiseFlightMath.direction(0,45).scale(speed));
                if(next.y<=4) assertTrue(CruiseFlightMath.passed(position,next,target,.05));
                position=next;
            }
            assertTrue(body.toString(),ticks<200);
        }
    }
    @Test public void modulesNeverMoveBodyOrExhaustSupportPoints() {
        for(CruisePartDefinition body:CruiseAirframes.bodies()) {
            CruiseBuild b=CruiseBuild.starter(body);Vec3d outlet=CruiseVisuals.exhaust(b,false),origin=CruiseVisuals.launchOrigin(b);
            for(CruisePartDefinition part:CruisePartDefinition.values()) if(part.getSlot()!=CruiseSlot.BODY && part.getSlot()!=CruiseSlot.LAUNCH) {
                b.set(part.getSlot(),part);
                assertEquals(outlet,CruiseVisuals.exhaust(b,true));assertEquals(origin,CruiseVisuals.launchOrigin(b));
                assertEquals(1,CruiseVisuals.radialScale(b),0);
            }
        }
    }
    @Test public void missileRemainsBelowBellyWithPositiveShortFairingGap() {
        for(UavAirframe frame:new UavAirframe[]{UavAirframe.RECON,UavAirframe.STRIKE}) {
            CruiseBuild b=CruiseBuild.starter(CruisePartDefinition.BODY_LIGHT);
            double top=UavCruiseCarriage.mountY(frame,b)+UavCruiseCarriage.storeTop(b);
            double gap=UavCruiseCarriage.attachY(frame)-top;
            assertTrue(gap>0 && gap<.65); // Full fin/underside clearance, not just the belly at the pylon.
            assertEquals(0,UavCruiseCarriage.groundLift(b),0);
        }
        CruiseBuild neptune=CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC);
        assertTrue(UavCruiseCarriage.mountY(UavAirframe.STRIKE,neptune)<UavCruiseCarriage.mountY(UavAirframe.STRIKE));
    }
    @Test public void launchClearanceCoversNativeNoseButNotItsOwnGroundFixture() {
        Vec3d block=new Vec3d(100.5,64,-20.5);
        for(CruisePartDefinition body:CruiseAirframes.bodies()) for(float yaw:new float[]{0,90,180,270}) {
            CruiseBuild b=CruiseBuild.starter(body);
            Vec3d start=block.add(CruiseVisuals.worldOffset(CruiseVisuals.launchOrigin(b),yaw,0));
            net.minecraft.util.math.AxisAlignedBB box=CruiseVisuals.launchClearance(b,start,yaw);
            assertTrue(box.minY>65);
            Vec3d nose=start.add(CruiseVisuals.worldOffset(new Vec3d(0,0,CruiseVisuals.noseOffset(b)),yaw,
                    b.get(CruiseSlot.LAUNCH)==CruisePartDefinition.LAUNCH_BOOSTER?-65:-12));
            assertTrue(body.toString(),box.contains(nose));
        }
    }
    private NBTTagCompound save(EntityCustomUav uav) throws Exception {
        Method method=EntityCustomUav.class.getDeclaredMethod("writeEntityToNBT",NBTTagCompound.class);method.setAccessible(true);
        NBTTagCompound tag=new NBTTagCompound();method.invoke(uav,tag);return tag;
    }
    private EntityCustomUav legacyCarrier(boolean flying) throws Exception {
        EntityCustomUav uav=new EntityCustomUav(null);uav.configure(UavBuild.cruiseCarrier(UavAirframe.RECON),null);
        CruiseBuild b=CruiseBuild.starter(CruisePartDefinition.BODY_LIGHT);b.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);
        ItemStack missile=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);b.writeToStack(missile);uav.setInventorySlotContents(0,missile);
        if(flying) uav.setLegacyState(2);
        NBTTagCompound tag=save(uav);tag.setDouble("UavCargoGroundLift",1.1);
        uav.setPosition(0,flying?64:1.1,0);
        Method read=EntityCustomUav.class.getDeclaredMethod("readEntityFromNBT",NBTTagCompound.class);read.setAccessible(true);read.invoke(uav,tag);
        return uav;
    }
    @Test public void parkedLegacyCarrierDropsOldServiceHeightWithoutLosingStore() throws Exception {
        EntityCustomUav uav=legacyCarrier(false);assertEquals(0,uav.posY,1e-9);assertTrue(uav.hasCruiseStore());
        assertEquals(0,save(uav).getDouble("UavCargoGroundLift"),0);
    }
    @Test public void flyingLegacyCarrierNeverTeleportsDuringHeightMigration() throws Exception {
        EntityCustomUav uav=legacyCarrier(true);assertEquals(64,uav.posY,0);assertTrue(uav.hasCruiseStore());
    }
}
