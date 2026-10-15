package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.silo.SiloDefense;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import net.minecraft.init.Bootstrap;
import org.junit.*;
import java.util.UUID;
import static org.junit.Assert.*;

public class SiloDefenseTest {
    @BeforeClass public static void bootstrap(){Bootstrap.register();}
    private EntityStrategicMissile target(StrategicSystemProfile profile){EntityStrategicMissile missile=new EntityStrategicMissile(null);missile.configureReentry(profile,0,100,80,100,new UUID(1,2),"red");return missile;}
    @Test public void everyTopolAndOreshnikIsEligibleRegardlessOfOrigin(){for(StrategicSystemProfile p:new StrategicSystemProfile[]{StrategicSystemProfile.TOPOL_M,StrategicSystemProfile.ORESHNIK}){EntityStrategicMissile m=target(p);assertTrue(SiloDefense.eligible(m));m.setSiloOrigin(true);assertTrue(SiloDefense.eligible(m));m.setLegacyState(2);assertFalse(SiloDefense.eligible(m));}}
    @Test public void fourthTierCannotBecomeGeneralAirDefense(){EntityStrategicMissile m=target(StrategicSystemProfile.ISKANDER);m.setSiloOrigin(true);assertFalse(SiloDefense.eligible(m));assertFalse(SiloDefense.eligible(new EntityWarTechAircraft(null)));assertFalse(SiloDefense.eligible(new EntitySiloInterceptor(null)));}
    @Test public void allOriginsUseFourthTierAndDoNotEnterOldChanceMatrix(){for(StrategicSystemProfile p:new StrategicSystemProfile[]{StrategicSystemProfile.TOPOL_M,StrategicSystemProfile.ORESHNIK})for(boolean silo:new boolean[]{false,true}){EntityStrategicMissile m=target(p);m.setSiloOrigin(silo);assertEquals(4,MissileTrackingService.getThreatTier(m));for(int tier=1;tier<=3;tier++)assertFalse(MissileTrackingService.canInterceptorEngage(m,tier,1600));assertTrue(MissileTrackingService.canInterceptorEngage(m,4,1600));}}
    @Test public void defensiveFlightIsNeverAnotherIncomingThreat(){assertEquals(0,MissileTrackingService.getThreatTier(new EntitySiloInterceptor(null)));EntityStrategicMissile m=target(StrategicSystemProfile.ORESHNIK);m.configureSiloLaunch(80,new UUID(3,4));assertTrue(m.isSiloOrigin());assertTrue(SiloDefense.eligible(m));}
    @Test public void fullFourthTierEnvelopeAccountsForHatchButRejectsLateTargets(){EntityStrategicMissile m=target(StrategicSystemProfile.TOPOL_M);m.setSiloOrigin(true);m.setPosition(1000,1500,100);assertTrue(SiloDefense.reachable(m,new net.minecraft.util.math.Vec3d(100,83,100),140));m.setPosition(110,120,100);assertFalse(SiloDefense.reachable(m,new net.minecraft.util.math.Vec3d(100,83,100),140));}
    @Test public void oldSavedWarheadsWithoutOriginAreStillAbmTargets() throws Exception{for(StrategicSystemProfile p:new StrategicSystemProfile[]{StrategicSystemProfile.TOPOL_M,StrategicSystemProfile.ORESHNIK}){EntityStrategicMissile original=target(p);net.minecraft.nbt.NBTTagCompound n=new net.minecraft.nbt.NBTTagCompound();original.writeToNBT(n);n.removeTag("SiloOrigin");n.removeTag("SiloAge");EntityStrategicMissile restored=new EntityStrategicMissile(null);java.lang.reflect.Method read=EntityStrategicMissile.class.getDeclaredMethod("readEntityFromNBT",net.minecraft.nbt.NBTTagCompound.class);read.setAccessible(true);read.invoke(restored,n);assertFalse(restored.isSiloOrigin());assertTrue(SiloDefense.eligible(restored));assertEquals(4,MissileTrackingService.getThreatTier(restored));}}
    @Test public void mobileAndSiloWarheadsHaveTheSamePhysicalInterceptWindow(){for(StrategicSystemProfile p:new StrategicSystemProfile[]{StrategicSystemProfile.TOPOL_M,StrategicSystemProfile.ORESHNIK}){EntityStrategicMissile mobile=target(p),silo=target(p);silo.setSiloOrigin(true);mobile.setPosition(400,700,100);silo.setPosition(400,700,100);net.minecraft.util.math.Vec3d origin=new net.minecraft.util.math.Vec3d(100,83,100);assertTrue(SiloDefense.reachable(mobile,origin,140));assertEquals(SiloDefense.reachable(silo,origin,140),SiloDefense.reachable(mobile,origin,140));assertEquals(SiloDefense.forecast(silo,160),SiloDefense.forecast(mobile,160));}}
}
