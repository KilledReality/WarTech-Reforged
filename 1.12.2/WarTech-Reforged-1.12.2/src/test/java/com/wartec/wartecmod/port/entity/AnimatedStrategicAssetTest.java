package com.wartec.wartecmod.port.entity;

import com.google.gson.Gson;
import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.gui.ContainerLegacyEntity;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.InventoryPlayer;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class AnimatedStrategicAssetTest {
    @BeforeClass public static void bootstrap() { Bootstrap.register(); }
    private AnimatedAssetScene scene(String name) throws Exception {
        return new Gson().fromJson(new String(Files.readAllBytes(Paths.get("src/main/resources/assets/wartecmod/models/strategic_animated/"+name+".json")),StandardCharsets.UTF_8),AnimatedAssetScene.class);
    }
    private int node(AnimatedAssetScene s,String prefix) {
        for(int i=0;i<s.nodes.length;i++) if(s.nodes[i].name.startsWith(prefix)) return i;
        throw new AssertionError(prefix);
    }
    @Test public void animatedTailAndServerSpawnSocketCoincideAtSeparation() throws Exception {
        for(StrategicSystemProfile system:new StrategicSystemProfile[]{StrategicSystemProfile.TOPOL_M,StrategicSystemProfile.ORESHNIK,StrategicSystemProfile.ISKANDER}) {
            AnimatedAssetScene s=scene(system.getId());
            for(int slot=0;slot<(system.isTactical()?2:1);slot++) {
                // Verify the runtime pose, including the intentionally higher dev84 throw.
                AnimatedAssetScene.Pose pose=TelLaunchGeometry.pose(s,system,1,system.getSeparationTick(),slot);
                float[] matrix=pose.worldMatrix(node(s,system.isTactical()?(slot==0?"Missile_L":"Missile_R"):"Missile"));
                net.minecraft.util.math.Vec3d expected=system.launchOffset(slot);
                assertEquals(system.getId()+" X",expected.x,matrix[12]*system.getModelScale(),.002);
                assertEquals(system.getId()+" Y",expected.y,matrix[13]*system.getModelScale(),.002);
                assertEquals(system.getId()+" Z",expected.z,matrix[14]*system.getModelScale(),.002);
            }
        }
    }
    @Test public void launchingRightTubeCannotResetLeftTubeOrHydraulics() throws Exception {
        AnimatedAssetScene s=scene("iskander");AnimatedAssetScene.Pose pose=s.pose();pose.apply("Prepare",5.25F,n->true);
        int left=node(s,"Missile_L"),pivot=node(s,"Erector_Pivot_L"),rod=node(s,"Lift_Rod_L");
        pose.apply("Launch_L",2,n->n.name.startsWith("Missile_L"));
        float[] a=pose.worldMatrix(left),b=pose.worldMatrix(pivot),c=pose.worldMatrix(rod);
        pose.apply("Launch_R",1,n->n.name.startsWith("Missile_R"));
        assertArrayEquals(a,pose.worldMatrix(left),0);assertArrayEquals(b,pose.worldMatrix(pivot),0);assertArrayEquals(c,pose.worldMatrix(rod),0);
    }
    @Test public void acceleratingLaunchPoseMatchesWorldSocketAtEveryFrame() throws Exception {
        for(StrategicSystemProfile system:new StrategicSystemProfile[]{StrategicSystemProfile.TOPOL_M,StrategicSystemProfile.ORESHNIK,StrategicSystemProfile.ISKANDER}) {
            AnimatedAssetScene s=scene(system.getId());
            for(int slot=0;slot<(system.isTactical()?2:1);slot++) for(float tick=1;tick<=system.getSeparationTick();tick+=.25F) {
                AnimatedAssetScene.Pose pose=TelLaunchGeometry.pose(s,system,1,tick,slot);
                float[] matrix=pose.worldMatrix(TelLaunchGeometry.missileNode(s,system,slot));
                net.minecraft.util.math.Vec3d socket=system.launchOffset(slot,tick);
                assertEquals(socket.x,matrix[12]*system.getModelScale(),.002);
                assertEquals(socket.y,matrix[13]*system.getModelScale(),.002);
                assertEquals(socket.z,matrix[14]*system.getModelScale(),.002);
            }
        }
    }
    @Test public void exportEaseOutCannotStopTheRocketBeforeHandoff() {
        for(StrategicSystemProfile system:StrategicSystemProfile.values()) if(system.isAvailable()) {
            double previousSpeed=0;
            for(int tick=system.getIgnitionTick()+1;tick<=system.getSeparationTick();tick++) {
                double speed=(system.launchTravel(tick)-system.launchTravel(tick-1))*system.getModelScale();
                assertTrue(speed>previousSpeed);assertTrue(speed<1.2);previousSpeed=speed;
            }
            assertEquals(previousSpeed,system.separationSpeed(),1e-9);
        }
    }
    @Test public void strategicNozzleClearsOriginalCanisterBeforeIgnition() throws Exception {
        for(StrategicSystemProfile system:new StrategicSystemProfile[]{StrategicSystemProfile.TOPOL_M,StrategicSystemProfile.ORESHNIK}) {
            AnimatedAssetScene s=scene(system.getId());
            AnimatedAssetScene.Pose pose=TelLaunchGeometry.pose(s,system,1,system.getIgnitionTick(),0);
            int canister=node(s,"Canister_LOD1");float[] m=pose.worldMatrix(canister);
            String group=s.meshes[s.nodes[canister].mesh][0].group,current="";double top=-Double.MAX_VALUE;
            for(String line:Files.readAllLines(Paths.get("src/main/resources/assets/wartecmod/models/strategic_animated/"+system.getId()+".obj"),StandardCharsets.UTF_8)) {
                if(line.startsWith("g ")) current=line.substring(2);
                if(current.equals(group) && line.startsWith("v ")) {
                    String[] xyz=line.split(" +");double x=Double.parseDouble(xyz[1]),y=Double.parseDouble(xyz[2]),z=Double.parseDouble(xyz[3]);
                    top=Math.max(top,(m[1]*x+m[5]*y+m[9]*z+m[13])*system.getModelScale());
                }
            }
            double nozzle=system.launchOffset(0,system.getIgnitionTick()).y;
            assertTrue(system.getId()+" nozzle="+nozzle+" canister top="+top,nozzle>top+1);
        }
    }
    @Test public void spawnPacketCarriesTubeIdentityBeforeAnyTrackingTick() {
        EntityStrategicTel tel=new EntityStrategicTel(null,StrategicSystemProfile.ISKANDER);
        EntityWarTechMissile missile=new EntityWarTechMissile(null);missile.getTelLaunchOrigin().configure(tel,1);
        io.netty.buffer.ByteBuf buffer=io.netty.buffer.Unpooled.buffer();
        try {
            missile.writeSpawnData(buffer);EntityWarTechMissile received=new EntityWarTechMissile(null);received.readSpawnData(buffer);
            assertEquals(tel.getEntityId(),received.getTelLaunchOrigin().launcherId);assertEquals(1,received.getTelLaunchOrigin().slot);
        } finally { buffer.release(); }
    }
    @Test public void realIskanderAmmoFitsBothTubesAndWrongWeaponsDoNot() {
        EntityStrategicTel tel=new EntityStrategicTel(null,StrategicSystemProfile.ISKANDER);
        ContainerLegacyEntity inventory=new ContainerLegacyEntity(new InventoryPlayer(null),tel,ContainerLegacyEntity.Layout.STRATEGIC);
        assertEquals(39,inventory.inventorySlots.size());
        for(int slot=0;slot<2;slot++) {
            assertTrue(inventory.getSlot(slot).isItemValid(new ItemStack(WarTechContent.ITEM_ISKANDER_MISSILE)));
            assertFalse(inventory.getSlot(slot).isItemValid(new ItemStack(WarTechContent.ITEM_TOMAHAWK_MISSILE)));
            assertFalse(inventory.getSlot(slot).isItemValid(new ItemStack(WarTechContent.STRATEGIC_MISSILE,1,0)));
            assertEquals(1,inventory.getSlot(slot).getSlotStackLimit());
        }
        tel.setInventorySlotContents(0,new ItemStack(WarTechContent.ITEM_ISKANDER_MISSILE));
        tel.setInventorySlotContents(1,new ItemStack(WarTechContent.ITEM_ISKANDER_MISSILE));assertEquals(3,tel.getMissileMask());
        tel.decrStackSize(1,1);assertEquals(1,tel.getMissileMask());
    }
    @Test public void strategicCanistersCannotCrossLoad() {
        EntityStrategicTel topol=new EntityStrategicTel(null,StrategicSystemProfile.TOPOL_M);
        assertTrue(topol.isCompatibleMissile(new ItemStack(WarTechContent.STRATEGIC_MISSILE,1,0)));
        assertFalse(topol.isCompatibleMissile(new ItemStack(WarTechContent.STRATEGIC_MISSILE,1,2)));
        assertFalse(topol.isCompatibleMissile(new ItemStack(WarTechContent.ITEM_ISKANDER_MISSILE)));
    }
}
