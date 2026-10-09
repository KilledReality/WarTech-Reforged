package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.gameplay.TileEntityCruiseLauncher;
import com.wartec.wartecmod.port.gui.ContainerCruiseProgrammer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.math.Vec3d;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class CruiseRevisionTest {
    @BeforeClass public static void bootstrap() {
        Bootstrap.register();
        net.minecraftforge.fml.common.registry.GameRegistry.registerTileEntity(TileEntityCruiseLauncher.class,new net.minecraft.util.ResourceLocation("wartecmod","cruise_launcher"));
    }
    private static CruiseBuild seeker() { CruiseBuild b=CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC);b.set(CruiseSlot.SEEKER,CruisePartDefinition.SEEKER_OPTICAL);return b; }
    private static ItemStack missile(CruiseBuild build) { ItemStack stack=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);build.writeToStack(stack);return stack; }
    @Test public void coordinateProgramHasExactlyOneGoalAndCanReplaceIt() {
        CruiseMission mission=new CruiseMission();assertTrue(mission.append(new Vec3d(100,64,0),0));
        assertFalse(mission.append(new Vec3d(200,64,0),0));assertEquals(1,mission.getTargets().size());
        assertTrue(mission.setTarget(new Vec3d(300,70,50),0));assertEquals(300,mission.getTargets().get(0).x,0);
    }
    @Test public void multipleSearchAreasRequireASeekerNotABrainTier() {
        CruiseMission mission=new CruiseMission();CruiseBuild build=CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC);
        assertFalse(mission.setMode(CruiseMission.Mode.SEARCH,build));build=seeker();
        assertTrue(mission.setMode(CruiseMission.Mode.SEARCH,build));
        for(int i=0;i<8;i++) assertTrue(mission.append(new Vec3d(300+i*100,64,50),0));
        assertFalse(mission.append(new Vec3d(2000,64,50),0));
        for(CruisePartDefinition brain:new CruisePartDefinition[]{CruisePartDefinition.NAV_COORDINATE,CruisePartDefinition.NAV_ROUTE,CruisePartDefinition.NAV_TERRAIN}) {
            build.set(CruiseSlot.NAVIGATION,brain);assertTrue(mission.isValidFor(build,0));
        }
        build.set(CruiseSlot.SEEKER,CruisePartDefinition.SEEKER_NONE);assertFalse(mission.isValidFor(build,0));
    }
    @Test public void modesAndSearchAreasPersistWithoutBecomingTransit() {
        CruiseMission mission=new CruiseMission();mission.setMode(CruiseMission.Mode.SEARCH,seeker());
        mission.append(new Vec3d(300,64,50),0);mission.append(new Vec3d(600,64,50),0);
        CruiseMission copy=CruiseMission.read(mission.write());assertEquals(CruiseMission.Mode.SEARCH,copy.getMode());assertEquals(mission.getTargets(),copy.getTargets());
        assertTrue(copy.setMode(CruiseMission.Mode.COORDINATE,seeker()));assertEquals(1,copy.getTargets().size());
    }
    @Test public void dev50TransitMigratesToItsFinalStrikeCoordinate() {
        NBTTagCompound old=new NBTTagCompound();old.setInteger("Dimension",0);NBTTagList route=new NBTTagList();
        for(int i=0;i<3;i++) { NBTTagCompound p=new NBTTagCompound();p.setDouble("X",300+i*100);p.setDouble("Y",64);p.setDouble("Z",50);route.appendTag(p); }
        old.setTag("Route",route);CruiseMission migrated=CruiseMission.read(old);
        assertEquals(CruiseMission.Mode.COORDINATE,migrated.getMode());assertEquals(1,migrated.getTargets().size());assertEquals(500,migrated.getTargets().get(0).x,0);
        assertTrue(migrated.isValidFor(CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC),0));
    }
    @Test public void unknownProgramSchemaAndModeAreRejected() {
        CruiseMission mission=new CruiseMission();mission.setTarget(new Vec3d(300,64,50),0);NBTTagCompound n=mission.write();
        n.setInteger("Schema",999);assertFalse(CruiseMission.read(n).isValidFor(seeker(),0));
        n=mission.write();n.setString("Mode","TRANSIT");assertFalse(CruiseMission.read(n).isValidFor(seeker(),0));
    }
    @Test public void rangeIsVisibleBeforeWingsAreInstalled() {
        CruiseBuild build=new CruiseBuild();build.set(CruiseSlot.BODY,CruisePartDefinition.BODY_CLASSIC);build.set(CruiseSlot.ENGINE,CruisePartDefinition.ENGINE_STANDARD);build.set(CruiseSlot.FUEL,CruisePartDefinition.FUEL_STANDARD);
        assertTrue(build.calculateStats().getRange()>0);assertFalse(build.calculateStats().isValid());
        int before=build.calculateStats().getRange();build.set(CruiseSlot.WINGS,CruisePartDefinition.WINGS_RANGE);assertTrue(build.calculateStats().getRange()>before);
    }
    @Test public void everyAddedMassChangesActualSpeedAndForcingChangesThrust() {
        CruiseBuild build=CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC);double before=build.calculateStats().getSpeed();
        build.set(CruiseSlot.LINK,CruisePartDefinition.LINK_COMMAND);assertTrue(build.calculateStats().getSpeed()<before);
        before=build.calculateStats().getSpeed();build.set(CruiseSlot.ENGINE,CruisePartDefinition.ENGINE_FAST);assertTrue(build.calculateStats().getSpeed()>before);
        before=build.calculateStats().getSpeed();build.set(CruiseSlot.FUEL,CruisePartDefinition.FUEL_EXTENDED);assertTrue(build.calculateStats().getSpeed()<before);
    }
    @Test public void launcherLoadingHasNoEntitySpawnAndPersistsLoadIdentity() throws Exception {
        TileEntityCruiseLauncher tile=new TileEntityCruiseLauncher();assertNull(tile.getLoadId());
        tile.setInventorySlotContents(0,missile(CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC)));assertNotNull(tile.getLoadId());
        UUID id=tile.getLoadId();NBTTagCompound save=tile.writeToNBT(new NBTTagCompound());
        // This headless suite does not run Forge item-registry events. Use a
        // registered vanilla carrier to test inventory/custom-NBT persistence.
        save.getTagList("Items",10).getCompoundTagAt(0).setString("id","minecraft:paper");
        TileEntityCruiseLauncher restored=new TileEntityCruiseLauncher();restored.readFromNBT(save);assertEquals(id,restored.getLoadId());
        assertFalse(restored.isEmpty());assertSame(CruisePartDefinition.BODY_CLASSIC,CruiseBuild.fromStack(restored.getStackInSlot(0)).getAirframe());
        tile.clear();assertNull(tile.getLoadId());tile.setInventorySlotContents(0,missile(CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC)));assertNotEquals(id,tile.getLoadId());
        String source=new String(Files.readAllBytes(Paths.get("src/main/java/com/wartec/wartecmod/port/gameplay/TileEntityCruiseLauncher.java")),StandardCharsets.UTF_8);
        String loading=source.substring(source.indexOf("public boolean load("),source.indexOf("public boolean launch("));
        assertFalse(loading.contains("spawnEntity"));assertTrue(source.contains("if(!world.spawnEntity(missile)) {"));
        assertTrue(source.contains("MissileChunkLoader.untrack(missile);return false;"));
    }
    @Test public void railAndBoosterFixturesRejectTheWrongKit() {
        CruiseBuild light=CruiseBuild.starter(CruisePartDefinition.BODY_LIGHT),classic=CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC);
        assertTrue(TileEntityCruiseLauncher.accepts(true,missile(light)));assertFalse(TileEntityCruiseLauncher.accepts(false,missile(light)));
        assertTrue(TileEntityCruiseLauncher.accepts(false,missile(classic)));assertFalse(TileEntityCruiseLauncher.accepts(true,missile(classic)));
        classic.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);assertFalse(TileEntityCruiseLauncher.accepts(false,missile(classic)));
    }
    @Test public void programmerHasRealDesignatorSlotAndLocksHeldMissile() {
        InventoryPlayer inventory=new InventoryPlayer(null);inventory.setInventorySlotContents(0,missile(seeker()));
        ContainerCruiseProgrammer container=new ContainerCruiseProgrammer(inventory,0);assertEquals(37,container.inventorySlots.size());
        assertTrue(container.getSlot(0).isItemValid(new ItemStack(WarTechContent.ITEM_TARGET_FINDER)));assertFalse(container.getSlot(0).isItemValid(missile(seeker())));
        assertFalse(container.getSlot(28).canTakeStack(null));assertFalse(container.getSlot(28).isItemValid(new ItemStack(WarTechContent.ITEM_TARGET_FINDER)));
        inventory.setInventorySlotContents(1,new ItemStack(WarTechContent.ITEM_TARGET_FINDER));assertFalse(container.transferStackInSlot(null,29).isEmpty());
        assertFalse(container.getDesignator().isEmpty());assertTrue(inventory.getStackInSlot(1).isEmpty());
        assertFalse(container.transferStackInSlot(null,0).isEmpty());assertTrue(container.getDesignator().isEmpty());
    }
    @Test public void basicBrainIsDirectAndNeverQueriesTerrain() {
        Vec3d target=new Vec3d(900,64,0);
        CruiseNavigation.Environment forbidden=new CruiseNavigation.Environment() { public boolean clear(Vec3d a,Vec3d b) { fail();return false; }public double height(double x,double z,double fallback) { fail();return 0; } };
        assertEquals(target,CruiseNavigation.aim(CruisePartDefinition.NAV_COORDINATE,new Vec3d(0,70,0),target,forbidden));
    }
    @Test public void betterBrainsPlanTheirOwnLocalTransitWithoutChangingGoals() {
        CruiseNavigation.Environment world=new CruiseNavigation.Environment() { public boolean clear(Vec3d a,Vec3d b) { return b.y>100; }public double height(double x,double z,double fallback) { return 100; } };
        Vec3d from=new Vec3d(0,70,0),target=new Vec3d(900,64,0);
        Vec3d route=CruiseNavigation.aim(CruisePartDefinition.NAV_ROUTE,from,target,world);assertTrue(route.y>from.y);assertNotEquals(target,route);
        Vec3d terrain=CruiseNavigation.aim(CruisePartDefinition.NAV_TERRAIN,from,target,world);assertTrue(terrain.y>=118);assertEquals(900,target.x,0);
    }
    @Test public void plannerWorkIsBoundedAndUnknownTerrainDoesNotTriggerLoad() {
        final int[] checks={0},heights={0};
        CruiseNavigation.Environment blocked=new CruiseNavigation.Environment() { public boolean clear(Vec3d a,Vec3d b) { checks[0]++;return false; }public double height(double x,double z,double fallback) { heights[0]++;return fallback; } };
        Vec3d aim=CruiseNavigation.aim(CruisePartDefinition.NAV_TERRAIN,new Vec3d(0,70,0),new Vec3d(20000,64,0),blocked);
        assertTrue(checks[0]<=CruiseNavigation.MAX_RAYS);assertTrue(heights[0]>=3 && heights[0]<=CruiseNavigation.MAX_HEIGHTS);assertTrue(aim.y<=248);
    }
    @Test public void bilingualDescriptionsExistForEveryPartAndAllGuiLabels() throws Exception {
        for(String language:new String[]{"en_us","ru_ru"}) {
            String lang=new String(Files.readAllBytes(Paths.get("src/main/resources/assets/wartecmod/lang/"+language+".lang")),StandardCharsets.UTF_8);
            for(CruisePartDefinition part:CruisePartDefinition.values()) { assertTrue(lang.contains("cruise.desc."+part.getId()+"="));assertTrue(lang.contains("cruise.detail."+part.getId()+"=")); }
            for(String key:new String[]{"gui.title","program.set_target","mode.search","launcher.launch","program.designator","stats.delta_range"}) assertTrue(lang.contains("cruise."+key+"="));
        }
    }
}
