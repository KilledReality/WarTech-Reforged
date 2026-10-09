package com.wartec.flightqa;

import com.google.gson.*;
import com.hbm.items.ModItems;
import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.*;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.network.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Actual Shift+RMB catapult starts, not a mid-flight beginRemoteControl shortcut. */
public final class ManualGeranQa {
    private WorldServer world;private Pilot player;private int tick,scenario;
    private TileEntityWarTechMachine tile;private BlockPos site;private Vec3d home,flightStart;
    private EntityWarTechMissile drone;private ItemStack loaded;private int launches;
    private final JsonArray cases=new JsonArray();private final List<String> failures=new ArrayList<>();
    private static final class Pilot extends FakePlayer {
        final List<String> messages=new ArrayList<>();
        Pilot(WorldServer world) { super(world,new GameProfile(new UUID(79,1),"ManualGeranQA")); }
        // Forge's default FakePlayer reports Vec3d.ZERO regardless of its position.
        @Override public Vec3d getPositionVector() { return new Vec3d(posX,posY,posZ); }
        @Override public void sendMessage(ITextComponent message) {
            if(messages!=null) messages.add(message instanceof TextComponentTranslation
                ? ((TextComponentTranslation)message).getKey():message.getUnformattedText());
        }
    }
    public void started() {
        if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED"))) throw new IllegalStateException("Not isolated");
        world=FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);
        if(!"flightqa".equals(world.getWorldInfo().getWorldName())) throw new IllegalStateException("Wrong world");
        world.getGameRules().setOrCreateGameRule("doMobSpawning","false");world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");
        player=new Pilot(world);player.connection=new net.minecraft.network.NetHandlerPlayServer(world.getMinecraftServer(),
            new net.minecraft.network.NetworkManager(net.minecraft.network.EnumPacketDirection.SERVERBOUND),player);
        player.capabilities.isCreativeMode=true;NetworkTeamHelper.setPlayerTeam(player,"blue");
        world.playerEntities.add(player);player.setPosition(0,4,0);world.getPlayerChunkMap().addPlayer(player);
        MinecraftForge.EVENT_BUS.register(this);
    }
    private void check(String name,boolean ok,String detail) {
        JsonObject c=new JsonObject();c.addProperty("kind",scenario+"_"+name);c.addProperty("success",ok);c.addProperty("detail",detail);cases.add(c);
        if(!ok) failures.add(scenario+"_"+name+": "+detail);
    }
    private void setup() {
        site=new BlockPos(32000+scenario*192,4,32000);
        for(int x=(site.getX()>>4)-2;x<=(site.getX()>>4)+2;x++) for(int z=(site.getZ()>>4)-2;z<=(site.getZ()>>4)+2;z++) world.getChunkFromChunkCoords(x,z);
        world.setBlockState(site,WarTechContent.GERAN_LAUNCHER.getDefaultState());
        WarTechContent.GERAN_LAUNCHER.onBlockPlacedBy(world,site,world.getBlockState(site),player,new ItemStack(WarTechContent.GERAN_LAUNCHER));
        tile=(TileEntityWarTechMachine)world.getTileEntity(site);tile.setOwnerTeam("blue");tile.setPower(100000);
        loaded=new ItemStack(scenario==1?WarTechContent.GERAN_DRONE:WarTechContent.GERAN_5_DRONE);tile.setInventorySlotContents(0,loaded);
        ItemStack d=new ItemStack(ModItems.designator_range);NBTTagCompound n=new NBTTagCompound();
        n.setInteger("xCoord",site.getX()+49);n.setInteger("zCoord",site.getZ()+297);
        if(scenario==2) n.setInteger("yCoord",3);
        if(scenario==5) n.setInteger("zCoord",site.getZ()+1100);
        d.setTagCompound(n);tile.setInventorySlotContents(1,d);
        home=new Vec3d(site.getX()+3.5,4,site.getZ()+.5);player.setPosition(home.x,home.y,home.z);
        player.setSneaking(true);player.setHeldItem(EnumHand.MAIN_HAND,ItemStack.EMPTY);player.setHeldItem(EnumHand.OFF_HAND,ItemStack.EMPTY);
        player.messages.clear();drone=null;launches=0;
        if(scenario<2) check("target_starts_unloaded",!world.isBlockLoaded(new BlockPos(n.getInteger("xCoord"),0,n.getInteger("zCoord"))),"XZ-only HBM range designator");
        if(scenario==3) tile.setPower(24999);
        if(scenario==4) tile.setOwnerTeam("red");
        if(scenario==8) tile.setInventorySlotContents(1,ItemStack.EMPTY);
        // Click a real multiblock part, as a player usually does on the visible rail.
        click(EnumHand.OFF_HAND);
        check("offhand_does_not_launch",tile.getPower()==(scenario==3?24999:100000)
            && !tile.getStackInSlot(0).isEmpty() && player.messages.isEmpty(),"no duplicate launch/error from second hand");
        click(EnumHand.MAIN_HAND);
        if(scenario<2) {
            check("pending_notice",player.messages.contains("geran.launch.resolving"),player.messages.toString());
            click(EnumHand.MAIN_HAND);click(EnumHand.MAIN_HAND);
            check("repeat_clicks_coalesced",Collections.frequency(player.messages,"geran.launch.resolving")==1,"one elevation operation");
            check("nothing_consumed_while_pending",tile.getPower()==100000 && !tile.getStackInSlot(0).isEmpty(),"commit only after target and control ready");
        }
        if(scenario==6) tile.setInventorySlotContents(0,new ItemStack(WarTechContent.GERAN_DRONE));
        if(scenario==7) player.setPosition(home.x+50,home.y,home.z);
        if(scenario==9) tile.setInventorySlotContents(1,new ItemStack(ModItems.designator_range));
        if(scenario==10) world.setBlockToAir(site);
    }
    private void click(EnumHand hand) {
        BlockPos part=site.add(0,0,1);
        check("click_handled_"+hand,WarTechContent.GERAN_LAUNCHER.onBlockActivated(world,part,
            world.getBlockState(part),player,hand,EnumFacing.UP,.5F,.5F,.5F),"multiblock resolves core");
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if(event.phase!=TickEvent.Phase.END) return;tick++;
        world.getPlayerChunkMap().updateMovingPlayer(player);
        int phase=(tick-10)%80;
        if(tick>=10 && scenario<11) {
            if(phase==0) setup();
            for(Entity e:new ArrayList<>(world.loadedEntityList)) if(e instanceof EntityWarTechMissile && !e.isDead) {
                EntityWarTechMissile m=(EntityWarTechMissile)e;
                if(m.isRemoteControlled() && drone==null) { drone=m;flightStart=m.getPositionVector();launches++; }
            }
            if(drone!=null && phase<35) drone.handleRemoteInput(player,-35,-8,1,0);
            if(phase==35) {
                if(scenario<3) {
                    check("manual_start_connected",drone!=null && drone.isRemoteControlled(),"actual catapult click -> pilot active");
                    check("one_drone_one_energy_charge",launches==1 && tile.getPower()==75000 && tile.getStackInSlot(0).isEmpty(),"launches="+launches+" power="+tile.getPower());
                    if(drone!=null) {
                        check("correct_type",new RemoteControlTelemetryMessage(drone).vehicleType==(scenario==1?1:8),drone.getMissileProfile().toString());
                        check("steering_and_takeoff",drone.posX>flightStart.x+3 && drone.posY>flightStart.y+3
                            && Double.isFinite(drone.posY),drone.getPositionVector().toString());
                        check("operator_concealed",player.noClip && player.isInvisible() && player.capabilities.disableDamage,"remote presence established");
                        drone.handleRemoteInput(player,drone.rotationYaw,drone.rotationPitch,1,2);
                        check("exit_resumes_autopilot",!drone.isRemoteControlled() && MissileChunkLoader.diagnostics(world).contains("flights=1/32"),MissileChunkLoader.diagnostics(world));
                    }
                } else {
                    String reason=scenario==3?"power":scenario==4?"iff":scenario==5?"range":scenario==6?"changed":scenario==7?"operator":scenario==8?"designator":scenario==9?"changed":"launcher";
                    check("specific_failure",player.messages.contains("geran.launch.error."+reason),player.messages.toString());
                    check("no_unintended_launch",drone==null && launches==0,"no accidental autonomous flight");
                    if(scenario!=10) check("no_consumption_on_failure",tile.getPower()==(scenario==3?24999:100000)
                        && !tile.getStackInSlot(0).isEmpty(),"ammo/energy retained");
                }
            }
            if(phase==55 && scenario<3) {
                check("pilot_restored",player.getPositionVector().squareDistanceTo(home)<.01 && !player.noClip
                    && !player.isInvisible() && !player.capabilities.disableDamage,"home="+home+" actual="+player.getPositionVector());
                if(drone!=null) drone.setDead();
            }
            if(phase==79) { if(drone!=null) drone.setDead();scenario++; }
        }
        if(tick==1000) {
            check("resources_released",OperationalChunks.pending(world)==0 && MissileChunkLoader.diagnostics(world).contains("flights=0/32")
                && MissileChunkLoader.diagnostics(world).contains("queue=0 chunks/0 owners"),MissileChunkLoader.diagnostics(world));
            JsonObject result=new JsonObject();result.addProperty("complete",true);result.add("cases",cases);result.add("failures",new Gson().toJsonTree(failures));
            result.addProperty("resources",MissileChunkLoader.diagnostics(world));
            Files.write(Paths.get("flightqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result).getBytes(StandardCharsets.UTF_8));
            System.out.println("FLIGHTQA manual Geran finished "+result);world.getPlayerChunkMap().removePlayer(player);world.playerEntities.remove(player);world.getMinecraftServer().initiateShutdown();
        }
    }
}
