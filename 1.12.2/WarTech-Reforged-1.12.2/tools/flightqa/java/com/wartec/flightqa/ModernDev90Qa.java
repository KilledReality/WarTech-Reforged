package com.wartec.flightqa;

import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.content.*;
import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

/** Native burst orders, stow sequencing and damage from two actual S8000 flights. */
final class ModernDev90Qa {
    private final WorldServer world;
    private final ModernQa host;
    private final List<Probe> probes=new ArrayList<>();
    static final class Operator extends FakePlayer {
        final List<String> messages=new ArrayList<>();
        Operator(WorldServer world) { super(world,new GameProfile(new UUID(90,1),"Dev90Operator")); }
        @Override public void sendMessage(ITextComponent message) {
            messages.add(message instanceof TextComponentTranslation?((TextComponentTranslation)message).getKey():message.getUnformattedText());
        }
    }
    ModernDev90Qa(WorldServer world,ModernQa host) throws Exception {
        this.world=world;this.host=host;ordersAndStow();
        probes.add(new Probe("air",14320,10600));probes.add(new Probe("ground",14500,10600));
    }
    private void ordersAndStow() throws Exception {
        Operator player=new Operator(world);player.setPosition(20000,4,20000);
        Method tick=EntityModernVehicle.class.getDeclaredMethod("serverTick",WarTechEntityProfile.class);tick.setAccessible(true);
        for(ModernVehicleProfile kind:ModernVehicleProfile.values()) if(kind.isMlrs()) {
            EntityModernVehicle v=new EntityModernVehicle(world,kind);v.setPosition(20000,4,20000);
            v.setOwnerIdentity(player.getUniqueID(),"dev90");v.setLegacyPower(v.getEnergyCapacity());
            for(int i=0;i<kind.slots;i++) v.setInventorySlotContents(i,new ItemStack(kind.ammunition()));
            NBTTagCompound ready=new NBTTagCompound();v.writeToNBT(ready);ready.setInteger("ModernDeployment",kind.prepareTicks);
            ready.setInteger("ModernReload",0);ready.setInteger("ModernCooldown",kind.interval);v.readFromNBT(ready);v.setDeployed(true);
            player.messages.clear();boolean accepted=true;
            for(int i=0;i<kind.slots;i++) accepted&=v.acceptArtilleryDesignatorTarget(player,new BlockPos(20000+i,4,20800+i));
            host.check("rapid_one_per_click_"+kind,accepted && v.getQueuedShots()==kind.slots && v.loadedCount()==kind.slots && player.messages.isEmpty(),"all clicks reserve exactly one different loaded tube even during physical interval");
            host.check("rapid_reserved_silent_"+kind,!v.acceptArtilleryDesignatorTarget(player,new BlockPos(20000,4,20800)) && player.messages.isEmpty(),"fully reserved magazine does not produce readiness/cooldown spam");
            NBTTagCompound saved=new NBTTagCompound();v.writeToNBT(saved);NBTTagList orders=saved.getTagList("ModernQueue",10);
            boolean targets=true;Set<UUID> ids=new HashSet<>();
            for(int i=0;i<orders.tagCount();i++) { NBTTagCompound o=orders.getCompoundTagAt(i);ids.add(o.getUniqueId("Id"));targets&=o.getInteger("Slot")==i && BlockPos.fromLong(o.getLong("Target")).equals(new BlockPos(20000+i,4,20800+i)); }
            EntityModernVehicle restored=new EntityModernVehicle(world,kind);restored.readFromNBT(saved);NBTTagCompound again=new NBTTagCompound();restored.writeToNBT(again);
            host.check("rapid_targets_restart_"+kind,targets && ids.size()==kind.slots && orders.equals(again.getTagList("ModernQueue",10)),"individual targets and unique shot IDs survive native NBT restore");
            v.handleLegacyGuiAction(4,player);v.setLegacyPower(0);player.messages.clear();
            host.check("rapid_power_reason_"+kind,!v.acceptArtilleryDesignatorTarget(player,new BlockPos(20000,4,20800)) && player.messages.equals(Collections.singletonList("wartec.modern.error.power")),player.messages);
            v.setLegacyPower(v.getEnergyCapacity());player.messages.clear();
            host.check("rapid_range_reason_"+kind,!v.acceptArtilleryDesignatorTarget(player,new BlockPos(20000,4,20001)) && player.messages.equals(Collections.singletonList("wartec.modern.error.range")),player.messages);
            v.setDeployed(false);player.messages.clear();
            host.check("rapid_deploy_reason_"+kind,!v.acceptArtilleryDesignatorTarget(player,new BlockPos(20000,4,20800)) && player.messages.equals(Collections.singletonList("wartec.modern.error.deploy")),player.messages);
            ready=new NBTTagCompound();v.writeToNBT(ready);ready.setFloat("ModernTurretYaw",120);ready.setInteger("ModernDeployment",kind.prepareTicks);v.readFromNBT(ready);
            tick.invoke(v,kind.entity);
            host.check("stow_aligns_before_lowering_"+kind,Math.abs(v.getTurretYaw()-118)<.01 && v.getDeployment()==1,"traverse returns over hull before package lowers");
            for(int i=0;i<kind.prepareTicks+65;i++) tick.invoke(v,kind.entity);
            host.check("stow_home_"+kind,v.getDeployment()==0 && v.getTurretYaw()==0,"all package rotations return to travel position");
            v.setDeployed(true);ready=new NBTTagCompound();v.writeToNBT(ready);ready.setInteger("ModernDeployment",kind.prepareTicks);v.readFromNBT(ready);
            for(int i=0;i<kind.slots;i++) v.setInventorySlotContents(i,ItemStack.EMPTY);player.messages.clear();
            host.check("rapid_empty_reason_"+kind,!v.acceptArtilleryDesignatorTarget(player,new BlockPos(20000,4,20800)) && player.messages.equals(Collections.singletonList("wartec.modern.error.empty")),player.messages);
            v.setDead();restored.setDead();
        }
    }
    final class Probe {
        final String name;final BlockPos target;final List<BlockPos> stone=new ArrayList<>();final EntityCow dummy;
        boolean hit,checked;Vec3d impact;float strength;
        Probe(String name,int x,int z) {
            this.name=name;target=new BlockPos(x,4,z);
            for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++) host.pin((x>>4)+dx,(z>>4)+dz);
            for(int dx=-8;dx<=8;dx++) for(int dz=-8;dz<=8;dz++) {
                BlockPos at=new BlockPos(x+dx,3,z+dz);stone.add(at);world.setBlockState(at,Blocks.STONE.getDefaultState(),3);
            }
            dummy=new EntityCow(world);dummy.setNoAI(true);dummy.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(1000);dummy.setHealth(1000);
            dummy.setPosition(x+3.5,4,z+.5);dummy.enablePersistence();
            host.check("banderol_"+name+"_dummy_spawn",world.spawnEntity(dummy),"persistent native living target");
        }
        void check() {
            int removed=0;for(BlockPos at:stone) if(world.getBlockState(at).getBlock()!=Blocks.STONE) removed++;
            host.check("banderol_"+name+"_damage",dummy.getHealth()<1000,"actual living-entity damage="+(1000-dummy.getHealth())+"; impact="+impact);
            host.check("banderol_"+name+"_destruction",removed>0,"actual stone floor cells removed="+removed);
            host.check("banderol_"+name+"_full_he",Math.abs(strength-10)<.01,"actual native explosion strength="+strength);
            checked=true;dummy.setDead();
        }
    }
    void impact(Vec3d point,float strength,List<net.minecraft.entity.Entity> entities) {
        for(Probe p:probes) if(!p.hit && point.squareDistanceTo(new Vec3d(p.target))<100) {
            p.hit=true;p.impact=point;p.strength=strength;
            System.out.println("MODERNQA BANDEROL_DIAGNOSTIC "+p.name+" cow="+p.dummy.getPositionVector()+" dead="+p.dummy.isDead+" loaded="+world.loadedEntityList.contains(p.dummy)+" affected="+entities.contains(p.dummy)+" density="+world.getBlockDensity(point,p.dummy.getEntityBoundingBox()));
        }
    }
    void tick() { for(Probe p:probes) if(p.hit && !p.checked) p.check(); }
    void finish() { host.check("banderol_both_damage_sites",probes.stream().allMatch(p->p.checked),"actual ground and air launch impacts evaluated after block/entity processing"); }
}
