package com.wartec.flightqa;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gui.*;
import com.wartec.wartecmod.port.planning.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Real Forge world, no user's saves. Includes saved pending commands and permissions. */
public final class TubeArtilleryQa {
    private WorldServer world;private int tick;private boolean resumed;
    private final List<EntityTubeArtillery> guns=new ArrayList<>();private final List<FakePlayer> players=new ArrayList<>();
    private final JsonArray cases=new JsonArray();private final List<String> failures=new ArrayList<>();
    private final Set<UUID> shots=new HashSet<>();private final int[] shotCounts=new int[2];
    private final EntityWarTechArtilleryProjectile[] firstShots=new EntityWarTechArtilleryProjectile[2];
    private static final Path SAVE=Paths.get("tube-restart.json");
    private static class Pilot extends FakePlayer {
        Pilot(WorldServer w,int n){super(w,new GameProfile(new UUID(114,n),"TubeQA"+n));}
        @Override public void setPositionAndUpdate(double x,double y,double z){setPosition(x,y,z);}
        @Override public void openGui(Object mod,int id,net.minecraft.world.World w,int x,int y,int z){Object c=new WarTechGuiHandler().getServerGuiElement(id,this,w,x,y,z);if(c instanceof net.minecraft.inventory.Container)openContainer=(net.minecraft.inventory.Container)c;}
    }
    private void check(String name,boolean ok,Object detail){JsonObject row=new JsonObject();row.addProperty("kind",name);row.addProperty("success",ok);row.addProperty("detail",String.valueOf(detail));cases.add(row);if(!ok)failures.add(name+": "+detail);System.out.println("TUBEQA "+row);}
    public void started()throws Exception{
        if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED")))throw new IllegalStateException("Not isolated");
        world=FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);resumed=Files.exists(SAVE);
        world.getGameRules().setOrCreateGameRule("doMobSpawning","false");world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");
        MinecraftForge.EVENT_BUS.register(this);
    }
    private void pin(int cx,int cz){ForgeChunkManager.Ticket t=ForgeChunkManager.requestTicket(com.wartec.wartecmod.WarTechReforged.instance,world,ForgeChunkManager.Type.NORMAL);if(t==null)throw new IllegalStateException("ticket");for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){world.getChunkFromChunkCoords(cx+x,cz+z);ForgeChunkManager.forceChunk(t,new ChunkPos(cx+x,cz+z));}}
    private void setup()throws Exception{
        pin(10000>>4,10000>>4);pin(10048>>4,10000>>4);
        for(int i=0;i<2;i++){
            TubeArtilleryProfile p=TubeArtilleryProfile.values()[i];EntityTubeArtillery gun=null;
            if(resumed){for(Entity e:world.loadedEntityList)if(e instanceof EntityTubeArtillery && ((EntityTubeArtillery)e).tubeProfile()==p){gun=(EntityTubeArtillery)e;break;}}
            if(gun==null){gun=new EntityTubeArtillery(world,p);gun.setPosition(10000+i*48,4.05,10000);gun.setVisual("ground/tube_"+p.model,0);world.spawnEntity(gun);}
            guns.add(gun);Pilot player=new Pilot(world,i+1);player.setPosition(gun.posX,4.05,gun.posZ);players.add(player);
            for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)world.setBlockState(gun.getPosition().add(x,-1,z),Blocks.BEDROCK.getDefaultState(),2);
            if(resumed){check("pending_survives_restart_"+p,gun.isFiring(),gun.writeToNBT(new NBTTagCompound()));continue;}
            check("empty_factory_power_"+p,gun.getLegacyPower()==0,"no free energy");gun.setOwnerIdentity(player.getUniqueID(),"");gun.setLegacyPower(10000);
            check("empty_factory_"+p,gun.ammunitionCount()==0,"no free ammunition");
            for(int type=0;type<12;type++)check("ammo_"+p+"_"+type,gun.isItemValidForSlot(0,new ItemStack(WarTechContent.ARTILLERY_AMMO,1,type)),"Greg ammunition");
            check("reject_rocket_"+p,!gun.isItemValidForSlot(0,new ItemStack(WarTechContent.GRAD_ROCKET)),"gun cannot fire rockets");
            gun.setInventorySlotContents(0,new ItemStack(WarTechContent.ARTILLERY_AMMO,4,0));
            check("inventory_preserves_stack_"+p,gun.getStackInSlot(0).getCount()==4,gun.getStackInSlot(0));
            check("range_low_"+p,!gun.programTarget(player,new BlockPos(gun.posX+10,4,gun.posZ)),"minimum range");
            check("range_high_"+p,!gun.programTarget(player,new BlockPos(gun.posX+p.range+10,4,gun.posZ)),"maximum range");
            check("target_"+p,gun.programTarget(player,new BlockPos(gun.posX+300,4,gun.posZ+150)),"manual coordinates");
            check("stowed_gate_"+p,!gun.handleLegacyGuiAction(0,player),"no shot until ready");
            FakePlayer stranger=new Pilot(world,90+i);stranger.setPosition(gun.posX,4,gun.posZ);gun.setOwnerIdentity(player.getUniqueID(),"");
            check("foreign_gui_"+p,!gun.isUsableByPlayer(stranger),"ownership");
            check("foreign_designator_"+p,!gun.acceptArtilleryDesignatorTarget(stranger,new BlockPos(gun.posX+300,4,gun.posZ)),"no remote access");
            check("foreign_link_"+p,!WarTechContent.ARTILLERY_TARGET_DESIGNATOR.linkTo(gun,stranger,new ItemStack(WarTechContent.ARTILLERY_TARGET_DESIGNATOR)),"binding rejects foreign player");
            check("deploy_"+p,gun.handleLegacyGuiAction(1,player),"real ground support");
            check("gui_factory_"+p,new WarTechGuiHandler().getServerGuiElement(WarTechGuiHandler.GUI_TUBE_ARTILLERY,player,world,gun.getEntityId(),0,0) instanceof ContainerTubeArtillery,"server interface");
            check("chassis_"+p,VehicleChassisProfile.isDrivable(gun.getProfile())==(p==TubeArtilleryProfile.CAESAR),"towed gun has no engine");
            check("recipe_"+p,com.wartec.wartecmod.port.production.ProductionCatalog.recipe("wartecmod:"+p.model+"@0")!=null,"paid assembly recipe");
        }
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent e)throws Exception{
        if(e.phase!=TickEvent.Phase.END)return;tick++;
        if(tick==20)setup();
        if(!resumed && tick==150)for(int i=0;i<2;i++){
            EntityTubeArtillery gun=guns.get(i);FakePlayer player=players.get(i);
            ItemStack designator=new ItemStack(WarTechContent.ARTILLERY_TARGET_DESIGNATOR);
            check("bind_"+i,WarTechContent.ARTILLERY_TARGET_DESIGNATOR.linkTo(gun,player,designator),designator);
            gun.handleLegacyGuiAction(11,player);check("empty_selected_slot_"+i,!gun.handleLegacyGuiAction(0,player) && gun.getStackInSlot(0).getCount()==4,"never switch payload without selecting it");gun.handleLegacyGuiAction(10,player);
            check("remote_one_shot_"+i,gun.acceptArtilleryDesignatorTarget(player,new BlockPos(gun.posX+300,4,gun.posZ+150)),gun.readiness());
            check("no_duplicate_"+i,!gun.acceptArtilleryDesignatorTarget(player,new BlockPos(gun.posX+300,4,gun.posZ+150)),"repeat click while aiming");
            check("inventory_locked_"+i,!new ContainerTubeArtillery(player.inventory,gun).inventorySlots.get(0).canTakeStack(player),"reserved ammo");
        }
        if(tick>150 && !resumed)for(Entity entity:new ArrayList<>(world.loadedEntityList))if(entity instanceof EntityWarTechArtilleryProjectile && shots.add(entity.getUniqueID())){
            EntityWarTechArtilleryProjectile shell=(EntityWarTechArtilleryProjectile)entity;TubeArtilleryProfile p=shell.tubeArtilleryProfile();if(p==null)continue;int i=p.ordinal();shotCounts[i]++;
            firstShots[i]=shell;
            check("shell_profile_"+i,shell.getAmmoType()==0 && shell.getProjectileKind()==0,"real Greg payload, custom visual");
            EntityTubeArtillery gun=guns.get(i);Vec3d[] socket=TubeArtilleryGeometry.muzzle(p,gun.posX,gun.posY,gun.posZ,gun.rotationYaw,gun.gunYaw(1),gun.gunPitch(1));
            Vec3d direction=new Vec3d(shell.motionX,shell.motionY,shell.motionZ).normalize();check("muzzle_axis_"+i,socket[1].dotProduct(direction)>.9999,socket[1]+" "+direction);
            check("consume_once_"+i,gun.getStackInSlot(0).getCount()==3 && gun.getLegacyPower()==9500,gun.getStackInSlot(0)+" "+gun.getLegacyPower());
        }
        if(!resumed && tick==430){
            for(int i=0;i<2;i++){
                EntityTubeArtillery gun=guns.get(i);check("exactly_one_"+i,shotCounts[i]==1,shotCounts[i]);
                EntityWarTechArtilleryProjectile shell=firstShots[i];check("physical_impact_"+i,shell!=null && shell.isDead && Math.hypot(shell.posX-(gun.posX+300.5),shell.posZ-(gun.posZ+150.5))<8,shell==null?"missing":shell.getPositionVector());
                check("planner_"+i,PlanningAssetRegistry.describe(gun).attack && PlanningAssetRegistry.describe(gun).ammo==3,"planning terminal sees artillery and real stocks");
                check("pending_for_restart_"+i,gun.acceptArtilleryDesignatorTarget(players.get(i),new BlockPos(gun.posX-400,4,gun.posZ)),gun.readiness());
            }
            Files.write(SAVE,new Gson().toJson(cases).getBytes(StandardCharsets.UTF_8));finish();
        }
        if(resumed && tick==300){for(int i=0;i<2;i++){EntityTubeArtillery gun=guns.get(i);check("restart_single_consumption_"+i,gun.getStackInSlot(0).getCount()==2 && gun.getLegacyPower()==9000,gun.getStackInSlot(0)+" "+gun.getLegacyPower());}finish();}
    }
    private void finish()throws Exception{JsonObject report=new JsonObject();report.addProperty("complete",true);report.addProperty("resumed",resumed);report.add("cases",cases);report.add("failures",new Gson().toJsonTree(failures));Files.write(Paths.get(resumed?"tube-restart-report.json":"tube-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(StandardCharsets.UTF_8));FMLCommonHandler.instance().getMinecraftServerInstance().initiateShutdown();}
}
