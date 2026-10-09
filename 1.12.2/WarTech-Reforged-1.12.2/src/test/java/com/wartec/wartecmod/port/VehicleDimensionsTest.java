package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.uav.*;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.Vec3d;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class VehicleDimensionsTest {
    @BeforeClass public static void boot() { Bootstrap.register(); }
    @Test public void fighterHierarchyIsModerateAndBomberIsCapped() {
        assertEquals(13.125,10.5*VehicleDimensions.scale(WarTechEntityProfile.F_16C),1e-5);
        assertEquals(15,12.5*VehicleDimensions.scale(WarTechEntityProfile.SU_27),1e-5);
        assertTrue(19.74*VehicleDimensions.scale(WarTechEntityProfile.TU_95)<22);
        assertEquals(13.5,18*VehicleDimensions.scale(WarTechEntityProfile.MQ_9_REAPER),1e-5);
    }
    @Test public void everyActiveProfileHasBoundedFiniteScale() {
        for(WarTechEntityProfile p:WarTechEntityProfile.values()) {
            assertTrue(VehicleDimensions.scale(p)>=.75 && VehicleDimensions.scale(p)<=1.25);
            assertTrue(Float.isFinite(p.getWidth()) && p.getWidth()>0);
            assertTrue(Float.isFinite(p.getHeight()) && p.getHeight()>0);
        }
    }
    @Test public void heavyUavRemainsBetweenReconAndReaperInSpan() {
        double small=2.87*VehicleDimensions.uavScale(UavAirframe.ONE_WAY);
        double recon=5.86*VehicleDimensions.uavScale(UavAirframe.RECON);
        double heavy=8.68*VehicleDimensions.uavScale(UavAirframe.STRIKE);
        assertTrue(small<recon && recon<heavy && heavy<13.5);
    }
    @Test public void cruiseSizeHierarchyDoesNotUseRealWorldGiantScales() {
        double[] lengths={3.60,5.16,5.20,4.80};double previous=0;int i=0;
        for(CruisePartDefinition body:CruiseAirframes.bodies()) {
            double length=lengths[i++]*CruiseAirframes.modelScale(body);
            assertTrue(length>previous && length<8);previous=length;
        }
    }
    @Test public void independentStoreFinEnvelopeStaysBelowNativeWing() {
        for(WarTechEntityProfile p:new WarTechEntityProfile[]{WarTechEntityProfile.F_16C,WarTechEntityProfile.SU_27,WarTechEntityProfile.TU_95})
            for(CruisePartDefinition body:CruiseAirframes.bodies()) {
                CruiseBuild b=CruiseBuild.starter(body);
                for(int slot=0;slot<CruiseAircraftLoadout.capacity(p,body);slot++) {
                    Vec3d at=CruiseAircraftLoadout.mount(p,b,slot);
                    assertTrue(CruiseAircraftLoadout.attachY(p,b,slot)-at.y-CruiseAircraftLoadout.storeEnvelopeTop(b)>=.119999);
                    assertTrue(at.y+CruiseAircraftLoadout.storeBodyTop(b)<CruiseAircraftLoadout.attachY(p,b,slot));
                }
            }
    }
    @Test public void bomberSixStrizhStoresHavePositiveWingtipClearance() {
        CruiseBuild b=CruiseBuild.starter(CruisePartDefinition.BODY_LONG_RANGE);
        for(int i=1;i<6;i++) assertTrue(CruiseAircraftLoadout.mount(WarTechEntityProfile.TU_95,b,i).x
            -CruiseAircraftLoadout.mount(WarTechEntityProfile.TU_95,b,i-1).x>2*.6561*CruiseAirframes.modelScale(b.getAirframe()));
    }
    @Test public void uavPylonsAreShortAndClearMissileFins() {
        for(UavAirframe frame:new UavAirframe[]{UavAirframe.RECON,UavAirframe.STRIKE})
            for(CruisePartDefinition body:new CruisePartDefinition[]{CruisePartDefinition.BODY_LIGHT,CruisePartDefinition.BODY_CLASSIC}) {
                // Recon carriers reject Neptune by airframe class, before mounting.
                if(frame==UavAirframe.RECON && body==CruisePartDefinition.BODY_CLASSIC) continue;
                CruiseBuild b=CruiseBuild.starter(body);double y=UavCruiseCarriage.mountY(frame,b);
                double gap=UavCruiseCarriage.attachY(frame)-y-UavCruiseCarriage.storeTop(b);
                assertTrue(gap>0 && gap<=.620001);
                assertTrue(y+CruiseAircraftLoadout.storeEnvelopeTop(b)<UavCruiseCarriage.attachY(frame));
            }
    }
    @Test public void conventionalUavReleaseMatchesMeasuredPylonTop() {
        for(int type=0;type<9;type++) for(int slot=0;slot<4;slot++) {
            Vec3d at=VehicleDimensions.uavStore(UavAirframe.STRIKE,type,slot);
            assertEquals(VehicleDimensions.uavWingY(UavAirframe.STRIKE,slot)-.08,at.y+VehicleDimensions.weaponTop(type),1e-9);
            assertEquals(-at.x,VehicleDimensions.uavStore(UavAirframe.STRIKE,type,3-slot).x,1e-9);
        }
    }
    @Test public void fixedBlockInfrastructureAndDisabledStrategicSystemsAreNotResized() {
        assertEquals(1,VehicleDimensions.blockScale("launchtube"),0);
        assertEquals(1,VehicleDimensions.blockScale("vlsexhaust"),0);
        assertEquals(1,VehicleDimensions.blockScale("ballisticmissilelauncher"),0);
        assertEquals(1,VehicleDimensions.scale(WarTechEntityProfile.STRATEGIC_TOPOL_M),0);
        assertEquals(1,VehicleDimensions.missileScale("slbm"),0);
        assertEquals(1,VehicleDimensions.missileScale("lrhw"),0);
    }
    @Test public void remoteCameraAndNativeDroneLauncherShareBodyScale() {
        assertEquals(VehicleDimensions.scale(WarTechEntityProfile.GERAN_2),VehicleDimensions.remoteScale(1),0);
        assertEquals(VehicleDimensions.remoteScale(1),VehicleDimensions.blockScale("geranlauncher"),0);
        assertEquals(VehicleDimensions.scale(WarTechEntityProfile.F_16C),VehicleDimensions.remoteScale(2),0);
        assertEquals(VehicleDimensions.uavScale(UavAirframe.RECON),VehicleDimensions.remoteScale(6),0);
    }
    @Test public void legacyMissileCoreUsesItsActualFamilyRatherThanStormFallback() {
        EntityWarTechMissile geran=new EntityWarTechMissile(null,com.wartec.wartecmod.port.content.MissileProfile.GERAN_2);
        assertEquals(1.15*1.20,geran.width,1e-6);assertEquals(.40*1.20,geran.height,1e-6);
        EntityWarTechMissile cj=new EntityWarTechMissile(null,com.wartec.wartecmod.port.content.MissileProfile.CJ10);
        assertEquals(.65*.58,cj.width,1e-6);
    }
    @Test public void customMissileCoreScalesOnBuildSynchronization() {
        for(CruisePartDefinition body:CruiseAirframes.bodies()) {
            EntityCustomCruise e=new EntityCustomCruise(null);
            net.minecraft.item.ItemStack s=new net.minecraft.item.ItemStack(com.wartec.wartecmod.port.content.WarTechContent.ASSEMBLED_CRUISE);
            CruiseBuild.starter(body).writeToStack(s);e.configure(s,null);
            assertEquals(.8*CruiseAirframes.modelScale(body),e.width,1e-6);
            assertEquals(.6*CruiseAirframes.modelScale(body),e.height,1e-6);
        }
    }
    @Test public void kh555UsesTheSameSixStationsAsOtherBomberWeapons() {
        // A projected wing-span box is too conservative for longitudinally staggered stores.
        // Full native triangles, gear and mixed neighbours are checked by the dev66 mesh audit.
        for(int i=0;i<6;i++) assertEquals(VehicleDimensions.tuStore(11,i).x,VehicleDimensions.tuStore(10,i).x,0);
        for(int i=1;i<6;i++) assertTrue(VehicleDimensions.tuStore(10,i).x-VehicleDimensions.tuStore(10,i-1).x>2.4);
        for(int code=10;code<=12;code++) for(int i=0;i<6;i++)
            assertTrue(VehicleDimensions.tuAttachY(code,i)-VehicleDimensions.tuStore(code,i).y-VehicleDimensions.tuStoreTop(code)>=.099999);
    }
    @Test public void legacyMissileCoreIsRestoredEvenWhenItsSpecificationDidNotChange() throws Exception {
        EntityWarTechMissile e=new EntityWarTechMissile(null,com.wartec.wartecmod.port.content.MissileProfile.CJ10);
        net.minecraft.nbt.NBTTagCompound tag=new net.minecraft.nbt.NBTTagCompound();
        java.lang.reflect.Method write=EntityWarTechMissile.class.getDeclaredMethod("writeEntityToNBT",net.minecraft.nbt.NBTTagCompound.class);
        java.lang.reflect.Method read=EntityWarTechMissile.class.getDeclaredMethod("readEntityFromNBT",net.minecraft.nbt.NBTTagCompound.class);
        write.setAccessible(true);read.setAccessible(true);write.invoke(e,tag);read.invoke(e,tag);
        assertEquals(.65*.58,e.width,1e-6);
    }
    @Test public void exportActualDimensionsForMeshAudit() throws Exception {
        com.google.gson.JsonObject root=new com.google.gson.JsonObject();
        com.google.gson.JsonObject profiles=new com.google.gson.JsonObject(),uavs=new com.google.gson.JsonObject(),cruise=new com.google.gson.JsonObject(),blocks=new com.google.gson.JsonObject(),missiles=new com.google.gson.JsonObject();
        for(WarTechEntityProfile p:WarTechEntityProfile.values()) profiles.addProperty(p.name(),VehicleDimensions.scale(p));
        for(UavAirframe f:UavAirframe.values()) uavs.addProperty(f.name(),VehicleDimensions.uavScale(f));
        for(CruisePartDefinition body:CruiseAirframes.bodies()) cruise.addProperty(CruiseVisuals.familyName(body),CruiseAirframes.modelScale(body));
        for(String name:new String[]{"geranlauncher","patriotlauncher","s400launcher","launchtube","vlsexhaust","ballisticmissilelauncher"}) blocks.addProperty(name,VehicleDimensions.blockScale(name));
        root.add("profiles",profiles);root.add("uav",uavs);root.add("cruise",cruise);root.add("blocks",blocks);
        for(com.wartec.wartecmod.port.content.MissileProfile m:com.wartec.wartecmod.port.content.MissileProfile.values()) missiles.addProperty(m.getIntentPath(),VehicleDimensions.missileScale(m.getIntentPath()));
        root.add("missiles",missiles);
        com.google.gson.JsonArray mounts=new com.google.gson.JsonArray();
        for(UavAirframe frame:new UavAirframe[]{UavAirframe.RECON,UavAirframe.STRIKE})
            for(CruisePartDefinition body:new CruisePartDefinition[]{CruisePartDefinition.BODY_LIGHT,CruisePartDefinition.BODY_CLASSIC}) {
                if(frame==UavAirframe.RECON && body==CruisePartDefinition.BODY_CLASSIC) continue;
                CruiseBuild b=CruiseBuild.starter(body);com.google.gson.JsonObject m=new com.google.gson.JsonObject();
                m.addProperty("frame",frame.name());m.addProperty("body",CruiseVisuals.familyName(body));
                m.addProperty("y",UavCruiseCarriage.mountY(frame,b));m.addProperty("attach",UavCruiseCarriage.attachY(frame));
                m.addProperty("body_top",UavCruiseCarriage.storeTop(b));m.addProperty("scale",CruiseAirframes.modelScale(body));mounts.add(m);
            }
        root.add("uav_mounts",mounts);
        com.google.gson.JsonArray conventional=new com.google.gson.JsonArray();
        for(int code=10;code<=12;code++) for(int slot=0;slot<6;slot++) {
            Vec3d v=VehicleDimensions.tuStore(code,slot);com.google.gson.JsonObject m=new com.google.gson.JsonObject();
            m.addProperty("code",code);m.addProperty("slot",slot);m.addProperty("x",v.x);m.addProperty("y",v.y);m.addProperty("z",v.z);
            m.addProperty("attach",VehicleDimensions.tuAttachY(code,slot));m.addProperty("body_top",VehicleDimensions.tuStoreBodyTop(code));
            m.addProperty("envelope_top",VehicleDimensions.tuStoreTop(code));conventional.add(m);
        }
        root.add("tu_stores",conventional);
        java.nio.file.Path path=java.nio.file.Paths.get("build/qa/vehicle-dimensions-dev64.json");java.nio.file.Files.createDirectories(path.getParent());
        java.nio.file.Files.write(path,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
