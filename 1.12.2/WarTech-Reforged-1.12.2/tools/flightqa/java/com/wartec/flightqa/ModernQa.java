package com.wartec.flightqa;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.*;
import com.wartec.wartecmod.port.integration.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Real server, saved launcher queues and moving Forge tickets in a disposable world. */
public final class ModernQa {
    private WorldServer world;private int tick,defenseTick,salvoTick;private boolean resumed;
    private final List<EntityModernVehicle> vehicles=new ArrayList<>();
    private final List<FakePlayer> players=new ArrayList<>();
    private final List<ForgeChunkManager.Ticket> tickets=new ArrayList<>();
    private JsonArray cases=new JsonArray();private final List<String> failures=new ArrayList<>();
    private final Set<UUID> shots=new HashSet<>();private JsonObject state;
    private final Set<MissileProfile> impacts=new HashSet<>();private boolean centralImpact;
    private NBTTagCompound queueBefore;
    private FakePlayer platformPlayer;
    private ModernDev88Qa dev88;
    private ModernDev90Qa dev90;
    private static final Path STATE=Paths.get("modern-restart-state.json");
    private static final class Pilot extends FakePlayer {
        Pilot(WorldServer world,int index) { super(world,new GameProfile(new UUID(87,index+1),"ModernQA"+index)); }
        @Override public void setPositionAndUpdate(double x,double y,double z) { setPosition(x,y,z); }
        @Override public void openGui(Object mod,int guiId,net.minecraft.world.World world,int x,int y,int z) {
            // Forge refuses to send fake players UI packets. Route the real server GUI factory locally.
            Object container=new com.wartec.wartecmod.port.gui.WarTechGuiHandler().getServerGuiElement(guiId,this,world,x,y,z);
            if(container instanceof net.minecraft.inventory.Container) openContainer=(net.minecraft.inventory.Container)container;
        }
    }
    public void started() throws Exception {
        if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED"))) throw new IllegalStateException("Not isolated");
        world=FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);
        world.getGameRules().setOrCreateGameRule("doMobSpawning","false");world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");
        resumed=Files.isRegularFile(STATE);
        if(resumed) { state=new JsonParser().parse(new String(Files.readAllBytes(STATE),StandardCharsets.UTF_8)).getAsJsonObject();cases=state.getAsJsonArray("cases"); }
        MinecraftForge.EVENT_BUS.register(this);
    }
    void check(String name,boolean success,Object detail) {
        JsonObject row=new JsonObject();row.addProperty("kind",name);row.addProperty("success",success);row.addProperty("detail",String.valueOf(detail));cases.add(row);
        if(!success) failures.add(name+": "+detail);System.out.println("MODERNQA "+row);
    }
    void pin(int cx,int cz) {
        ForgeChunkManager.Ticket ticket=ForgeChunkManager.requestTicket(com.wartec.wartecmod.WarTechReforged.instance,world,ForgeChunkManager.Type.NORMAL);
        if(ticket==null) throw new IllegalStateException("No QA terrain ticket");tickets.add(ticket);
        for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) { world.getChunkFromChunkCoords(cx+x,cz+z);ForgeChunkManager.forceChunk(ticket,new ChunkPos(cx+x,cz+z)); }
    }
    private void setup() {
        for(int i=0;i<7;i++) pin((10000+i*24)>>4,10000>>4);
        if(resumed) return;
        for(int i=0;i<7;i++) {
            ModernVehicleProfile kind=ModernVehicleProfile.values()[i];
            EntityModernVehicle vehicle=new EntityModernVehicle(world,kind);vehicle.setPosition(10000+i*24,4,10000);vehicle.setVisual("ground/modern_"+kind.model,0);
            FakePlayer player=new Pilot(world,i);player.setPosition(vehicle.posX,4,vehicle.posZ);vehicle.setOwnerIdentity(player.getUniqueID(),"modernqa");vehicle.setLegacyPower(vehicle.getEnergyCapacity());
            world.spawnEntity(vehicle);vehicles.add(vehicle);players.add(player);
            check("profile_"+kind,VehicleChassisProfile.isDrivable(vehicle.getProfile()),vehicle.getProfile());
            player.startRiding(vehicle,true);check("crew_"+kind,vehicle.getCollisionBox(player)==null && !vehicle.canPassengerSteer(),"shared collision and server input");
            player.dismountRidingEntity();
            for(int slot=0;slot<kind.slots;slot++) vehicle.setInventorySlotContents(slot,new ItemStack(kind.ammunition()));
            if(kind.isMlrs()) {
                for(int row=0;row<vehicle.targetCount();row++) check("target_"+kind+"_"+row,vehicle.programTarget(player,row,(int)vehicle.posX+row*9,3,10800+row*9,kind==ModernVehicleProfile.GRAD?50:0),"actual range and ownership");
                FakePlayer stranger=new Pilot(world,40);stranger.setPosition(vehicle.posX,4,vehicle.posZ);
                check("owner_"+kind,!vehicle.programTarget(stranger,0,10000,3,10800,0),"foreign operator rejected");
            }
            check("deploy_"+kind,vehicle.handleLegacyGuiAction(1,player),"native ground support");
            check("reload_gate_"+kind,!kind.isMlrs() || !vehicle.handleLegacyGuiAction(0,player),"reload/preparation blocks firing without consuming stores");
            if(kind.isMlrs()) {
                BlockPos point=vehicle.getTarget(0);ItemStack designator=new ItemStack(WarTechContent.ITEM_TARGET_FINDER);NBTTagCompound target=new NBTTagCompound();
                target.setInteger("xCoord",point.getX());target.setInteger("yCoord",point.getY());target.setInteger("zCoord",point.getZ());designator.setTagCompound(target);
                vehicle.setInventorySlotContents(kind.slots+1,designator);
                check("import_accepted_"+kind,vehicle.importTarget(player,0),"real saved designator item uses asynchronous import");
            } else check("auto_default_"+kind,vehicle.getLegacyFireMode()==0,"operator explicitly enables automatic defense");
        }
        check("buk_tier",vehicles.get(4).getRequiredInterceptorTier()==2,"WTI-2, excludes strategic vehicles");
        ItemStack banderol=new ItemStack(WarTechContent.S8000_MISSILE);
        check("banderol_ground",TileEntityCruiseLauncher.accepts(false,banderol) && !TileEntityCruiseLauncher.accepts(true,banderol),"cruise platform and no Geran rail");
        for(WarTechEntityProfile carrier:new WarTechEntityProfile[]{WarTechEntityProfile.F_16C,WarTechEntityProfile.SU_27,WarTechEntityProfile.TU_95}) {
            EntityWarTechAircraft aircraft=new EntityWarTechAircraft(world,carrier);check("banderol_carrier_"+carrier,aircraft.isPayloadCompatible(banderol),"actual carrier slot accepts item");
        }
        testLaunchPlatforms();
        try { dev88=new ModernDev88Qa(world,this,new Pilot(world,88));dev88.setup(); }
        catch(Exception error) { throw new IllegalStateException("Dev88 setup",error); }
        try { ModernDev89Qa.run(world,this,new Pilot(world,89)); }
        catch(Exception error) { throw new IllegalStateException("Dev89 setup",error); }
        try { dev90=new ModernDev90Qa(world,this); }
        catch(Exception error) { throw new IllegalStateException("Dev90 setup",error); }
    }
    private void testLaunchPlatforms() {
        platformPlayer=new Pilot(world,60);
        pin(14000>>4,10000>>4);
        TileEntityWarTechMachine geran=platform(WarTechContent.GERAN_LAUNCHER,new BlockPos(14000,4,10000));
        TileEntityWarTechMachine ballistic=platform(WarTechContent.BALLISTIC_MISSILE_LAUNCHER,new BlockPos(14008,4,10000));
        TileEntityWarTechMachine tube=platform(WarTechContent.LAUNCH_TUBE,new BlockPos(14016,4,10000));
        check("fp5_platform",geran.isItemValidForSlot(0,new ItemStack(WarTechContent.FP5_MISSILE)),"Geran platform accepts Flamingo");
        check("fp7_platform",ballistic.isItemValidForSlot(0,new ItemStack(WarTechContent.FP7_MISSILE)) && !tube.isItemValidForSlot(0,new ItemStack(WarTechContent.FP7_MISSILE)),"ballistic block only");
        check("fp7_not_tel",!new EntityStrategicTel(world,StrategicSystemProfile.ISKANDER).isCompatibleMissile(new ItemStack(WarTechContent.FP7_MISSILE)),"does not fit the Iskander vehicle");
        check("fp5_not_ballistic",!ballistic.isItemValidForSlot(0,new ItemStack(WarTechContent.FP5_MISSILE)),"platform roles enforced");
        for(TileEntityWarTechMachine tile:new TileEntityWarTechMachine[]{geran,ballistic}) {
            ItemStack ammo=new ItemStack(tile==geran?WarTechContent.FP5_MISSILE:WarTechContent.FP7_MISSILE);
            ItemStack designator=new ItemStack(WarTechContent.ITEM_TARGET_FINDER);NBTTagCompound tag=new NBTTagCompound();
            tag.setInteger("xCoord",tile.getPos().getX());tag.setInteger("yCoord",4);tag.setInteger("zCoord",tile==geran?10600:11200);designator.setTagCompound(tag);
            tile.setInventorySlotContents(0,ammo);tile.setInventorySlotContents(1,designator);tile.setPower(tile.getMaxPower());
            platformPlayer.setPosition(tile.getPos().getX()+2,4,tile.getPos().getZ());
            check(tile==geran?"fp5_actual_launch":"fp7_actual_launch",tile.launchLoadedMissile(platformPlayer) && tile.getStackInSlot(0).isEmpty(),"native platform launches and consumes exactly one real missile");
        }
        pin(14320>>4,10000>>4);
        ItemStack banderol=new ItemStack(WarTechContent.S8000_MISSILE);CruiseMission mission=new CruiseMission();mission.setTarget(new Vec3d(14320,4,10600),0);mission.writeToStack(banderol);
        EntityCustomCruise rocket=new EntityCustomCruise(world);rocket.setPosition(14320,50,10000);rocket.configure(banderol,null);rocket.setOwnerTeam("modernqa");
        check("banderol_actual_flight",EntityCustomCruise.launchError(world,banderol,rocket.getPositionVector())==null && MissileChunkLoader.spawnFlight(rocket),"fixed S8000 build flies through native cruise system");
        // Full-strength payload assertions need distinct impact sites: otherwise
        // one earlier HE impact can destroy another test missile during its approach.
        mission.setTarget(new Vec3d(14500,4,10600),0);mission.writeToStack(banderol);
        BlockPos cruisePos=new BlockPos(14024,4,10000);world.setBlockState(cruisePos,WarTechContent.CRUISE_LAUNCH_POINT.getDefaultState(),3);
        TileEntityCruiseLauncher cruise=(TileEntityCruiseLauncher)world.getTileEntity(cruisePos);cruise.setInventorySlotContents(0,banderol.copy());platformPlayer.setPosition(14024,4,10002);
        check("banderol_ground_launch",cruise.launch(platformPlayer) && cruise.getStackInSlot(0).isEmpty(),"native ground booster launch with full missile clearance");
    }
    private TileEntityWarTechMachine platform(net.minecraft.block.Block block,BlockPos pos) {
        world.setBlockState(pos,block.getDefaultState(),3);block.onBlockPlacedBy(world,pos,world.getBlockState(pos),platformPlayer,new ItemStack(block));
        return (TileEntityWarTechMachine)world.getTileEntity(pos);
    }
    private void startSalvos() throws Exception {
        List<EntityWarTechMissile> reservations=new ArrayList<>();
        for(int i=0;i<24;i++) { EntityWarTechMissile missile=LegacyEntityFactory.missile(world,MissileProfile.GRAD_ROCKET);missile.setPosition(10000,20,10000);reservations.add(missile);check("offensive_reserve_"+i,MissileChunkLoader.prepare(missile),"24 offensive slots"); }
        EntityWarTechMissile excess=LegacyEntityFactory.missile(world,MissileProfile.GRAD_ROCKET);excess.setPosition(10000,20,10000);check("offensive_limit",!MissileChunkLoader.prepare(excess),"25th offensive flight rejected");
        List<EntityWarTechMissile> aa=new ArrayList<>();for(int i=0;i<8;i++) { EntityWarTechMissile missile=LegacyEntityFactory.missile(world,MissileProfile.ANTI_AIR_TIER_2);missile.setPosition(10000,20,10000);aa.add(missile);check("aa_reserve_"+i,MissileChunkLoader.prepare(missile),"eight AA slots remain available"); }
        for(int i=0;i<4;i++) {
            EntityModernVehicle vehicle=vehicles.get(i);check("salvo_"+vehicle.modernProfile(),vehicle.handleLegacyGuiAction(0,players.get(i)),"real full magazine queue");
            Method tick=EntityModernVehicle.class.getDeclaredMethod("tickQueue");tick.setAccessible(true);tick.invoke(vehicle);
            check("busy_preserves_"+i,vehicle.loadedCount()==vehicle.modernProfile().slots && vehicle.getQueuedShots()==vehicle.modernProfile().slots,"no ammo spent when admission fails");
        }
        queueBefore=new NBTTagCompound();vehicles.get(0).writeToNBT(queueBefore);
        for(Entity missile:reservations) MissileChunkLoader.untrack(missile);for(Entity missile:aa) MissileChunkLoader.untrack(missile);MissileChunkLoader.untrack(excess);
    }
    private void observe() {
        for(Entity entity:world.loadedEntityList) if(entity.getEntityData().hasUniqueId("ModernShot")) shots.add(entity.getEntityData().getUniqueId("ModernShot"));
    }
    @SubscribeEvent public void explosion(net.minecraftforge.event.world.ExplosionEvent.Detonate event) throws Exception {
        if(event.getWorld()!=world)return;
        Entity source=null;Vec3d point=event.getExplosion().getPosition();
        for(Entity candidate:world.loadedEntityList) if((candidate instanceof EntityWarTechMissile || candidate instanceof EntityCustomCruise) && candidate.getPositionVector().squareDistanceTo(point)<.01) { source=candidate;break; }
        if(source instanceof EntityWarTechMissile) {
            EntityWarTechMissile missile=(EntityWarTechMissile)source;ModernMissileProfile profile=ModernMissileProfile.of(missile.getMissileProfile());
            // Incoming defense fixtures may explode as damaged wreckage after an
            // intercept. Their reduced blast is not an offensive payload impact.
            if(missile.getEntityData().getBoolean("Dev88PayloadQa")) {
                Field strength=net.minecraftforge.fml.relauncher.ReflectionHelper.findField(net.minecraft.world.Explosion.class,"size","explosionSize","field_77280_f");
                int variant=missile.getVisualVariant();float factor=variant==0?1:variant==1?.45F:variant==2?.55F:variant==3?.75F:.4F;
                check("payload_actual_blast_"+profile+"_"+variant,Math.abs(strength.getFloat(event.getExplosion())-profile.strength*factor)<.01,"actual strength="+strength.getFloat(event.getExplosion()));return;
            }
            if(missile.getEntityData().getBoolean("ModernQaThreat")) return;
            if(profile!=null && impacts.add(profile.missile)) {
                Field strength=net.minecraftforge.fml.relauncher.ReflectionHelper.findField(net.minecraft.world.Explosion.class,"size","explosionSize","field_77280_f");
                check("impact_strength_"+profile,Math.abs(strength.getFloat(event.getExplosion())-profile.strength)<.01,"actual ExplosionLarge strength="+strength.getFloat(event.getExplosion()));
                Field pathField=EntityWarTechMissile.class.getDeclaredField("modernPath");pathField.setAccessible(true);
                ModernFlightPath path=(ModernFlightPath)pathField.get(missile);
                double terminalError=path==null?Double.POSITIVE_INFINITY:Math.hypot(missile.posX-path.target.x,missile.posZ-path.target.z);
                check("impact_reaches_target_"+profile,terminalError<Math.max(2,profile.speed*2),"actual terminal error="+terminalError+"; nominal error includes configured CEP="+Math.hypot(missile.posX-missile.getTargetX(),missile.posZ-missile.getTargetZ()));
            }
            if(missile.getEntityData().getBoolean("ModernExactAim")) {
                centralImpact=Math.hypot(missile.posX-missile.getTargetX(),missile.posZ-missile.getTargetZ())<2;
                check("grad_central_impact",centralImpact,"first area rocket lands at programmed centre");
            }
        }
        if(source instanceof EntityCustomCruise && ((EntityCustomCruise)source).getBuild().isS8000()) {
            Field strength=net.minecraftforge.fml.relauncher.ReflectionHelper.findField(net.minecraft.world.Explosion.class,"size","explosionSize","field_77280_f");
            check("banderol_impact_strength",Math.abs(strength.getFloat(event.getExplosion())-10)<.01,"actual HE 10");impacts.add(MissileProfile.S8000);
            if(dev90!=null) dev90.impact(point,strength.getFloat(event.getExplosion()),event.getAffectedEntities());
        }
    }
    private void saveAndStop() throws Exception {
        if(dev90!=null) dev90.finish();
        check("restart_in_flight",vehicles.get(0).getQueuedShots()>0 && !shots.isEmpty(),"save occurs inside a 40-rocket salvo");
        JsonObject saved=new JsonObject();saved.add("cases",cases);saved.add("failures",new Gson().toJsonTree(failures));JsonArray ids=new JsonArray();
        for(EntityModernVehicle vehicle:vehicles) {
            JsonObject row=new JsonObject();row.addProperty("uuid",vehicle.getUniqueID().toString());row.addProperty("ammo",vehicle.loadedCount());row.addProperty("queued",vehicle.getQueuedShots());
            row.addProperty("x",vehicle.posX);row.addProperty("z",vehicle.posZ);ids.add(row);
        }
        saved.add("vehicles",ids);saved.add("seen_shots",new Gson().toJsonTree(shots));
        Files.write(STATE,new GsonBuilder().setPrettyPrinting().create().toJson(saved).getBytes(StandardCharsets.UTF_8));
        System.out.println("MODERNQA CHECKPOINT");world.getMinecraftServer().initiateShutdown();
    }
    private void restoreVehicles() {
        for(JsonElement element:state.getAsJsonArray("vehicles")) {
            JsonObject row=element.getAsJsonObject();Entity entity=world.getEntityFromUuid(UUID.fromString(row.get("uuid").getAsString()));
            if(!(entity instanceof EntityModernVehicle)) throw new IllegalStateException("Saved launcher missing "+row);
            EntityModernVehicle vehicle=(EntityModernVehicle)entity;vehicles.add(vehicle);FakePlayer player=new Pilot(world,players.size());player.setPosition(vehicle.posX,4,vehicle.posZ);players.add(player);
            check("restored_queue_"+vehicle.modernProfile(),vehicle.getQueuedShots()<=row.get("queued").getAsInt() && vehicle.loadedCount()<=row.get("ammo").getAsInt(),"saved ammo and queue resumed without growing");
        }
        for(JsonElement id:state.getAsJsonArray("seen_shots")) shots.add(UUID.fromString(id.getAsString()));
    }
    private void defenseChecks() throws Exception {
        EntityModernVehicle buk=vehicles.get(4),osa=vehicles.get(5),radar=vehicles.get(6);
        Method eligible=EntityModernVehicle.class.getDeclaredMethod("eligible",Entity.class);eligible.setAccessible(true);
        for(int index=0;index<2;index++) {
            EntityModernVehicle vehicle=index==0?osa:buk;
            for(MissileProfile profile:new MissileProfile[]{MissileProfile.FP5,MissileProfile.FP7,MissileProfile.GRAD_ROCKET,MissileProfile.HIMARS_ROCKET}) {
                EntityWarTechMissile target=LegacyEntityFactory.missile(world,profile);target.setPosition(vehicle.posX+65,35,vehicle.posZ+35);target.setOwnerTeam("hostile");
                boolean expected=vehicle.modernProfile().tier>=com.wartec.wartecmod.port.network.MissileTrackingService.getThreatTier(target);
                check("tier_"+vehicle.modernProfile()+"_"+profile,((Boolean)eligible.invoke(vehicle,target))==expected,"actual targeting predicate");
            }
            EntityStrategicMissile strategic=new EntityStrategicMissile(world);strategic.setPosition(vehicle.posX+65,35,vehicle.posZ+35);strategic.configureBoost(StrategicSystemProfile.TOPOL_M,11000,3,10000,null,"hostile");
            for(ModernMissileProfile profile:ModernMissileProfile.values()) if(profile.isMlrs()) for(int variant=0;variant<5;variant++) {
                EntityWarTechMissile target=LegacyEntityFactory.missile(world,profile.missile);target.setVisual("missile/"+profile.missile.getIntentPath(),variant);target.setPosition(vehicle.posX+65,35,vehicle.posZ+35);target.setOwnerTeam("hostile");
                boolean expected=vehicle.modernProfile().tier>=(profile==ModernMissileProfile.HIMARS?2:1);
                check("aa_matrix_"+vehicle.modernProfile()+"_"+profile+"_"+variant,((Boolean)eligible.invoke(vehicle,target))==expected,"actual hostile target predicate for every MLRS payload");
                target.setOwnerIdentity(vehicle.getOwnerUuid(),vehicle.getOwnerTeam());
                check("aa_friendly_"+vehicle.modernProfile()+"_"+profile+"_"+variant,!(Boolean)eligible.invoke(vehicle,target),"friendly rocket excluded");
            }
            check("strategic_excluded_"+vehicle.modernProfile(),!(Boolean)eligible.invoke(vehicle,strategic),"strategic MBR is outside AA tier 1/2");
            check("aa_auto_enabled_"+vehicle.modernProfile(),vehicle.handleLegacyGuiAction(5,players.get(index==0?5:4)) && vehicle.getLegacyFireMode()==1,"actual authorized GUI action");
        }
        for(int index=0;index<2;index++) {
            EntityModernVehicle vehicle=index==0?osa:buk;
            // Keep the whole sensor-to-target ray loaded, not only its endpoints.
            pin(((int)vehicle.posX)>>4,((int)vehicle.posZ+80)>>4);
            if(index==1) pin(((int)vehicle.posX)>>4,((int)vehicle.posZ+160)>>4);
            EntityWarTechMissile target=LegacyEntityFactory.missile(world,index==0?MissileProfile.FP5:MissileProfile.FP7);
            target.getEntityData().setBoolean("ModernQaThreat",true);
            target.setPosition(vehicle.posX,35,vehicle.posZ+(index==0?95:175));target.setOwnerTeam("hostile");target.configureLegacyGroundLaunch((int)vehicle.posX,3,(int)vehicle.posZ-600);
            check("aa_real_target_"+vehicle.modernProfile(),MissileChunkLoader.spawnFlight(target) && (Boolean)eligible.invoke(vehicle,target),"live missile spawn, loaded sight line and targeting predicate; hp="+target.getHealthValue());
            if(index==0) {
                Method defense=EntityModernVehicle.class.getDeclaredMethod("tickDefense");defense.setAccessible(true);defense.invoke(vehicle);
                check("osa_reserves_first_target",vehicle.getCapSlot()==0,"Buk must skip this nearer reserved target and acquire the other live missile");
            }
        }
        check("radar_emits",radar.isLegacyOperational() && ElectronicWarfareService.getEmitter(world,radar.getEntityId())!=null,"9S18 has an ARM-visible active emitter");
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if(event.phase!=TickEvent.Phase.END)return;tick++;
        if(tick==1) setup();
        if(dev90!=null) dev90.tick();
        if(!resumed && dev88!=null) dev88.verify(tick);
        if(!resumed && tick==20) for(int i=0;i<4;i++) check("import_applied_"+vehicles.get(i).modernProfile(),vehicles.get(i).getTargetRevision(0)==2,"same-coordinate server import acknowledges a new revision");
        if(resumed && tick==20) restoreVehicles();
        if(!resumed && salvoTick==0 && tick>=720 && (dev88.finished && MissileChunkLoader.availableFlightSlots(world)==32 || tick==6000)) {
            check("platform_flights_finished",MissileChunkLoader.availableFlightSlots(world)==32,"capacity test starts after all earlier platform flights release their resources");
            startSalvos();salvoTick=tick;
        }
        observe();
        if(!resumed && salvoTick>0 && tick==salvoTick+60) { saveAndStop();return; }
        boolean salvosFinished=resumed && vehicles.size()==7 && vehicles.subList(0,4).stream().allMatch(v->v.getQueuedShots()==0);
        if(resumed && defenseTick==0 && tick>=900 && (salvosFinished || tick==1800)) {
            for(int i=0;i<4;i++) check("finished_salvo_"+i,vehicles.get(i).loadedCount()==0 && vehicles.get(i).getQueuedShots()==0,"all actual missiles consumed once");
            check("unique_shots",shots.size()==70,"observed "+shots.size()+" of 40+6+12+12");
            for(ModernMissileProfile profile:new ModernMissileProfile[]{ModernMissileProfile.GRAD,ModernMissileProfile.HIMARS,ModernMissileProfile.URAGAN,ModernMissileProfile.SMERCH}) check("real_impact_"+profile,impacts.contains(profile.missile),"salvo reaches its target and creates its configured explosion");
            defenseChecks();
            defenseTick=tick;
        }
        if(resumed && defenseTick>0 && tick==defenseTick+20)
            check("radar_real_scope_contacts",vehicles.get(6).getLegacyBlipCount()>0 && vehicles.get(6).getLegacyBlip(0)!=0,"9S18 scope uses actual tracking sweep positions");
        if(resumed && defenseTick>0 && tick==defenseTick+200) {
            for(int i=4;i<6;i++) check("aa_fired_"+i,vehicles.get(i).loadedCount()<6 && vehicles.get(i).getCapSlot()<0,"real AA launch and closed cap after exit; ammo="+vehicles.get(i).loadedCount()+" cap="+vehicles.get(i).getCapSlot()+" energy="+vehicles.get(i).getLegacyPower());
            for(Entity entity:new ArrayList<>(world.loadedEntityList)) if(entity instanceof EntityWarTechBase) entity.setDead();
            for(ForgeChunkManager.Ticket ticket:tickets) ForgeChunkManager.releaseTicket(ticket);tickets.clear();
        }
        if(resumed && defenseTick>0 && tick==defenseTick+430) {
            String resources=MissileChunkLoader.diagnostics(world);check("resources_released",resources.contains("flights=0/") && resources.contains("queue=0 chunks/0 owners"),resources);
            JsonObject report=new JsonObject();report.addProperty("complete",true);report.add("cases",cases);report.add("failures",new Gson().toJsonTree(failures));report.addProperty("resources",resources);
            report.addProperty("actual_restart",true);report.addProperty("unique_shots",shots.size());
            Files.write(Paths.get("flightqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(StandardCharsets.UTF_8));
            System.out.println("MODERNQA FINISHED failures="+failures);world.getMinecraftServer().initiateShutdown();
        }
    }
}
