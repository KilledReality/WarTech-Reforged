package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.gameplay.TileEntityCruiseLauncher;
import java.util.UUID;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class CruiseLauncherTest {
    @BeforeClass public static void bootstrap() {
        Bootstrap.register();
        net.minecraftforge.fml.common.registry.GameRegistry.registerTileEntity(TileEntityCruiseLauncher.class,new net.minecraft.util.ResourceLocation("wartecmod","cruise_launcher"));
    }
    private ItemStack missile(CruisePartDefinition body) {
        ItemStack s=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);CruiseBuild.starter(body).writeToStack(s);
        CruiseMission m=new CruiseMission();m.setTarget(new Vec3d(500,64,800),0);m.writeToStack(s);return s;
    }
    @Test public void allFourGroundBodiesHaveAnAcceptingFixture() {
        assertEquals(4,CruiseAirframes.bodies().length);
        for(CruisePartDefinition body:CruiseAirframes.bodies())
            assertTrue(body.name(),TileEntityCruiseLauncher.accepts(body==CruisePartDefinition.BODY_LIGHT,missile(body)));
    }
    @Test public void wrongFixturesAndAirAdaptersRemainRejected() {
        for(CruisePartDefinition body:CruiseAirframes.bodies()) {
            ItemStack s=missile(body);
            assertFalse(TileEntityCruiseLauncher.accepts(body!=CruisePartDefinition.BODY_LIGHT,s));
            CruiseBuild b=CruiseBuild.fromStack(s);b.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);b.writeToStack(s);
            assertFalse(TileEntityCruiseLauncher.accepts(true,s));assertFalse(TileEntityCruiseLauncher.accepts(false,s));
        }
    }
    @Test public void invalidItemsAndMalformedBuildsAreRejected() {
        assertFalse(TileEntityCruiseLauncher.accepts(false,ItemStack.EMPTY));
        assertFalse(TileEntityCruiseLauncher.accepts(false,new ItemStack(Items.PAPER)));
        assertFalse(TileEntityCruiseLauncher.accepts(false,new ItemStack(WarTechContent.ASSEMBLED_CRUISE)));
        ItemStack s=missile(CruisePartDefinition.BODY_CLASSIC);s.getTagCompound().getCompoundTag(CruiseBuild.TAG).setInteger("Schema",99);
        assertFalse(TileEntityCruiseLauncher.accepts(false,s));
    }
    @Test public void loadingPreservesMissionAndGivesEachLoadANewChainIdentity() {
        TileEntityCruiseLauncher tile=new TileEntityCruiseLauncher();ItemStack s=missile(CruisePartDefinition.BODY_CLASSIC);
        NBTTagCompound tag=s.getTagCompound().copy();tile.setInventorySlotContents(0,s.copy());UUID first=tile.getLoadId();
        assertNotNull(first);assertEquals(tag,tile.getStackInSlot(0).getTagCompound());
        tile.removeStackFromSlot(0);assertTrue(tile.isEmpty());assertNull(tile.getLoadId());
        tile.setInventorySlotContents(0,s.copy());assertNotEquals(first,tile.getLoadId());
    }
    @Test public void savingLauncherPreservesMissileAndLoadIdentity() {
        TileEntityCruiseLauncher source=new TileEntityCruiseLauncher();source.setInventorySlotContents(0,missile(CruisePartDefinition.BODY_LONG_RANGE));
        NBTTagCompound tag=source.writeToNBT(new NBTTagCompound());
        // Forge's mod-item registry event does not run in this headless suite.
        tag.getTagList("Items",10).getCompoundTagAt(0).setString("id","minecraft:paper");
        TileEntityCruiseLauncher restored=new TileEntityCruiseLauncher();restored.readFromNBT(tag);
        assertEquals(source.getLoadId(),restored.getLoadId());assertEquals(source.getStackInSlot(0).getTagCompound(),restored.getStackInSlot(0).getTagCompound());
        restored.clear();assertNull(restored.getLoadId());assertTrue(restored.isEmpty());
    }
    @Test public void onlyFriendlyCruiseBombletsAreProtectedNotOrdinaryShells() {
        com.wartec.wartecmod.port.entity.EntityWarTechArtilleryProjectile source=new com.wartec.wartecmod.port.entity.EntityWarTechArtilleryProjectile(null);
        com.wartec.wartecmod.port.entity.EntityWarTechArtilleryProjectile friend=new com.wartec.wartecmod.port.entity.EntityWarTechArtilleryProjectile(null);
        com.wartec.wartecmod.port.entity.EntityWarTechArtilleryProjectile ordinary=new com.wartec.wartecmod.port.entity.EntityWarTechArtilleryProjectile(null);
        source.configureCruiseSubmunition(4);friend.configureCruiseSubmunition(5);
        UUID owner=new UUID(71,1);source.setOwnerIdentity(owner,"blue");friend.setOwnerIdentity(owner,"blue");ordinary.setOwnerIdentity(owner,"blue");
        net.minecraft.util.DamageSource damage=new net.minecraft.util.EntityDamageSource("explosion",source).setExplosion();
        assertTrue(friend.isCruiseSubmunition());assertFalse(friend.isArmed());assertFalse(ordinary.isCruiseSubmunition());
        assertTrue(com.wartec.wartecmod.port.integration.StrikeBlastSafety.ignores(friend,damage));
        assertFalse(com.wartec.wartecmod.port.integration.StrikeBlastSafety.ignores(ordinary,damage));
        friend.setOwnerIdentity(new UUID(71,2),"red");
        assertFalse(com.wartec.wartecmod.port.integration.StrikeBlastSafety.ignores(friend,damage));
    }
    @Test public void bombletIdentitySurvivesSaveWithoutArmingKineticRodPayload() throws Exception {
        com.wartec.wartecmod.port.entity.EntityWarTechArtilleryProjectile source=new com.wartec.wartecmod.port.entity.EntityWarTechArtilleryProjectile(null);
        source.configureCruiseSubmunition(5);NBTTagCompound tag=new NBTTagCompound();
        java.lang.reflect.Method write=source.getClass().getDeclaredMethod("writeEntityToNBT",NBTTagCompound.class);write.setAccessible(true);write.invoke(source,tag);
        com.wartec.wartecmod.port.entity.EntityWarTechArtilleryProjectile restored=new com.wartec.wartecmod.port.entity.EntityWarTechArtilleryProjectile(null);
        java.lang.reflect.Method read=source.getClass().getDeclaredMethod("readEntityFromNBT",NBTTagCompound.class);read.setAccessible(true);read.invoke(restored,tag);
        assertTrue(restored.isCruiseSubmunition());assertFalse(restored.isArmed());assertEquals(.2,restored.width,.001);
    }
}
