package com.wartec.flightqa;

import com.google.gson.*;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyMachine;
import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.TileEntityPlanningTerminal;
import com.wartec.wartecmod.port.gui.*;
import com.wartec.wartecmod.port.integration.NetworkTeamHelper;
import com.wartec.wartecmod.port.planning.*;
import com.wartec.wartecmod.port.production.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.*;
import net.minecraftforge.common.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Paid HBM product -> normal placement -> terminal wizard -> native shots -> real restart. */
public final class TubePlanningQa {
    private static final Path CHECKPOINT=Paths.get("tube-planning-restart.json");
    private final JsonArray cases=new JsonArray();
    private final List<String> failures=new ArrayList<>();
    private final EntityTubeArtillery[] guns=new EntityTubeArtillery[2];
    private final AttackOperation[] plans=new AttackOperation[2],cancelPlans=new AttackOperation[2],restartPlans=new AttackOperation[2];
    private final Map<UUID,EntityWarTechArtilleryProjectile> flights=new LinkedHashMap<>();
    private final Map<UUID,BlockPos> targets=new HashMap<>();
    private WorldServer world;private Pilot owner,enemy;private int tick,stage,stageTick;private boolean resumed;
    private static class Pilot extends FakePlayer {
        Pilot(WorldServer w,int n){super(w,new GameProfile(new UUID(200,n),"TubePlanQA"+n));interactionManager.setGameType(GameType.SURVIVAL);}
        @Override public void closeScreen(){openContainer=inventoryContainer;}
        @Override public void setPositionAndUpdate(double x,double y,double z){setPosition(x,y,z);}
        @Override public Vec3d getPositionVector(){return new Vec3d(posX,posY,posZ);}
        @Override public void sendMessage(ITextComponent message){}
    }
    private void check(String name,boolean ok,Object detail){JsonObject row=new JsonObject();row.addProperty("kind",name);row.addProperty("success",ok);row.addProperty("detail",String.valueOf(detail));cases.add(row);if(!ok)failures.add(name+": "+detail);System.out.println("TUBEPLANNINGQA "+row);}
    public void started(){
        if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED")))throw new IllegalStateException("Not isolated");
        world=FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);resumed=Files.exists(CHECKPOINT);
        world.getGameRules().setOrCreateGameRule("doMobSpawning","false");world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");
        MinecraftForge.EVENT_BUS.register(this);
    }
    private void pin(int x,int z){ForgeChunkManager.Ticket ticket=ForgeChunkManager.requestTicket(com.wartec.wartecmod.WarTechReforged.instance,world,ForgeChunkManager.Type.NORMAL);if(ticket==null)throw new IllegalStateException("No QA ticket");for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){world.getChunkFromChunkCoords((x>>4)+dx,(z>>4)+dz);ForgeChunkManager.forceChunk(ticket,new ChunkPos((x>>4)+dx,(z>>4)+dz));}}
    private ItemStack manufacture(TubeArtilleryProfile profile){
        JsonObject row=ProductionCatalog.recipe("wartecmod:"+profile.model+"@0");
        GenericRecipe recipe=AssemblyMachineRecipes.INSTANCE.recipeNameMap.get(ProductionRecipes.name(row));
        check("registered_recipe_"+profile,recipe!=null,ProductionRecipes.name(row));if(recipe==null)throw new IllegalStateException("Missing recipe");
        TileEntityMachineAssemblyMachine tile=new TileEntityMachineAssemblyMachine();tile.setWorld(world);tile.setPos(new BlockPos(10000,4,9990));
        tile.assemblerModule.setRecipe(ProductionRecipes.name(row),false);tile.assemblerModule.setupTanks(recipe);
        for(int i=0;i<recipe.inputItem.length;i++){AStack input=recipe.inputItem[i];ItemStack ingredient=input.getStackList().get(0).copy();ingredient.setCount(input.count());tile.inventory.setStackInSlot(4+i,ingredient);}
        tile.assemblerModule.update(1,1,true,ItemStack.EMPTY);check("no_free_craft_"+profile,tile.inventory.getStackInSlot(16).isEmpty() && tile.assemblerModule.progress==0,"unpowered HBM machine");
        long initial=(long)(recipe.duration+4)*recipe.power;tile.power=initial;int ticks=0;
        while(tile.inventory.getStackInSlot(16).isEmpty() && ticks++<recipe.duration+5)tile.assemblerModule.update(1,1,true,ItemStack.EMPTY);
        ItemStack product=tile.inventory.getStackInSlot(16).copy();
        check("real_hbm_product_"+profile,ItemStack.areItemStacksEqual(product,ProductionCatalog.output(row)),product);
        check("paid_energy_"+profile,initial-tile.power==(long)ticks*recipe.power && ticks>=recipe.duration,initial-tile.power);
        boolean consumed=true;for(int i=4;i<16;i++)consumed&=tile.inventory.getStackInSlot(i).isEmpty();check("paid_materials_"+profile,consumed,"all exact inputs consumed");return product;
    }
    private void setup(){
        pin(10000,10000);pin(10072,10000);pin(10450,10160);pin(9670,10000);
        owner=new Pilot(world,1);enemy=new Pilot(world,2);NetworkTeamHelper.setPlayerTeam(owner,"blue");NetworkTeamHelper.setPlayerTeam(enemy,"red");
        BlockPos terminalPos=new BlockPos(10000,4,10000);
        JsonObject saved=resumed?new JsonParser().parse(read(CHECKPOINT)).getAsJsonObject():null;
        for(int i=0;i<2;i++){
            TubeArtilleryProfile profile=TubeArtilleryProfile.values()[i];int x=10024+i*48;
            if(resumed){guns[i]=(EntityTubeArtillery)world.getEntityFromUuid(UUID.fromString(saved.getAsJsonArray("guns").get(i).getAsString()));if(guns[i]==null)throw new IllegalStateException("Saved gun missing");
                restartPlans[i]=PlanningWorldData.get(world).operations.get(UUID.fromString(saved.getAsJsonArray("operations").get(i).getAsString()));
                check("pending_terminal_order_restored_"+profile,guns[i].isFiring() && restartPlans[i]!=null && restartPlans[i].steps.get(1).phase==AttackOperation.Phase.ACCEPTED,"same entity, operation and accepted step");continue;}
            for(int dx=-8;dx<=8;dx++)for(int dz=-8;dz<=8;dz++)world.setBlockState(new BlockPos(x+dx,3,10000+dz),Blocks.BEDROCK.getDefaultState(),2);
            owner.setPosition(x,4,10000);owner.setHeldItem(EnumHand.MAIN_HAND,manufacture(profile));
            EnumActionResult placed=profile.item().onItemUse(owner,world,new BlockPos(x,3,10000),EnumHand.MAIN_HAND,EnumFacing.UP,.5F,1,.5F);
            for(Entity e:new ArrayList<>(world.loadedEntityList))if(e instanceof EntityTubeArtillery && ((EntityTubeArtillery)e).tubeProfile()==profile)guns[i]=(EntityTubeArtillery)e;
            check("crafted_item_places_"+profile,placed==EnumActionResult.SUCCESS && guns[i]!=null && owner.getHeldItemMainhand().isEmpty(),placed);
            if(guns[i]==null)throw new IllegalStateException("Crafted product did not place");
            check("crafted_gun_empty_"+profile,guns[i].getLegacyPower()==0 && guns[i].ammunitionCount()==0,"no factory power or shells");
            guns[i].setLegacyPower(10000);guns[i].setInventorySlotContents(0,new ItemStack(WarTechContent.ARTILLERY_AMMO,2,1));guns[i].setInventorySlotContents(2,new ItemStack(WarTechContent.ARTILLERY_AMMO,3,0));
            guns[i].setInventorySlotContents(5,new ItemStack(WarTechContent.ARTILLERY_TARGET_DESIGNATOR));
        }
        owner.setPosition(10000.5,4,10000.5);enemy.setPosition(10000.5,4,10000.5);
        if(!resumed){world.setBlockState(terminalPos,WarTechContent.PLANNING_TERMINAL.getDefaultState(),3);WarTechContent.PLANNING_TERMINAL.onBlockPlacedBy(world,terminalPos,world.getBlockState(terminalPos),owner,new ItemStack(WarTechContent.PLANNING_TERMINAL));}
        TileEntityPlanningTerminal terminal=(TileEntityPlanningTerminal)world.getTileEntity(terminalPos);
        owner.openContainer=new ContainerPlanningTerminal(owner,terminal,null);owner.openContainer.windowId=87;
        check("terminal_session",owner.openContainer.canInteractWith(owner),"real server container");
        if(!resumed){enemy.openContainer=new ContainerPlanningTerminal(enemy,terminal,null);enemy.openContainer.windowId=87;check("foreign_terminal_denied",PlanningService.handle(enemy,87,new NBTTagCompound())==null,"server scope recheck");}
    }
    private static String read(Path path){try{return new String(Files.readAllBytes(path),StandardCharsets.UTF_8);}catch(Exception e){throw new RuntimeException(e);}}
    private NBTTagCompound request(String action,AttackOperation plan){
        NBTTagCompound n=new NBTTagCompound();n.setString("Action",action);n.setInteger("Tab",1);n.setDouble("X",10000);n.setDouble("Z",10000);n.setInteger("Radius",512);
        if(plan!=null){n.setUniqueId("OperationId",plan.id);n.setUniqueId("Selected",plan.id);n.setInteger("Revision",plan.revision);if(action.equals("save"))n.setTag("Operation",plan.write());}
        return PlanningService.handle(owner,87,n);
    }
    private AttackOperation plan(int i,int count,boolean reverse){
        AttackOperation operation=new AttackOperation();operation.scope=PlanningAccessPolicy.scope(owner);operation.author=owner.getUniqueID();operation.title=(reverse?"Restart/cancel ":"Wizard ")+guns[i].tubeProfile();
        NBTTagCompound asset=PlanningAssetRegistry.describe(guns[i]).write();List<NBTTagCompound> weapons=AttackPlanBuilder.weapons(asset,operation);NBTTagCompound selected=null;
        for(NBTTagCompound weapon:weapons)if(weapon.getString("Item").equals("wartecmod:artilleryammo") && weapon.getInteger("Meta")==0)selected=weapon;
        if(selected==null)for(NBTTagCompound weapon:weapons)if(weapon.getInteger("Meta")==0 && weapon.getInteger("Slot")==2)selected=weapon;
        check("wizard_actual_shells_"+tick+"_"+i,selected!=null && selected.getInteger("Available")==guns[i].getStackInSlot(2).getCount(),weapons);
        if(selected==null)throw new IllegalStateException("Wizard shells missing");
        check("wizard_excludes_designator_"+tick+"_"+i,weapons.size()==2,"only two actual shell payloads");
        int x=(int)guns[i].posX+(reverse?-350:400),z=(int)guns[i].posZ+(reverse?0:160);
        operation.steps.addAll(AttackPlanBuilder.build(operation,asset,selected.getString("WeaponKey"),count,x,4,z,0,10,false));
        check("wizard_explicit_slot_"+tick+"_"+i,operation.steps.size()==count+1 && operation.steps.stream().filter(s->s.action==AttackOperation.Action.LAUNCH).allMatch(s->s.slot==2 && s.count==1),operation.write());
        return operation;
    }
    private AttackOperation save(AttackOperation operation){NBTTagCompound response=request("save",operation);check("save_terminal_"+tick,response!=null && response.getString("Error").isEmpty(),response==null?"null":response.getString("Error"));AttackOperation stored=PlanningWorldData.get(world).operations.get(operation.id);if(stored==null)throw new IllegalStateException("Terminal save failed");return stored;}
    private void start(AttackOperation operation){NBTTagCompound response=request("start",operation);check("start_terminal_"+tick,response!=null && response.getString("Error").isEmpty() && operation.state==AttackOperation.State.RUNNING,response==null?"null":response.getString("Error"));}
    @SubscribeEvent public void joined(EntityJoinWorldEvent event){
        if(event.getWorld()!=world || !(event.getEntity() instanceof EntityWarTechArtilleryProjectile))return;
        EntityWarTechArtilleryProjectile shell=(EntityWarTechArtilleryProjectile)event.getEntity();NBTTagCompound n=shell.getEntityData();if(!n.hasUniqueId("PlanningOperation"))return;
        AttackOperation operation=PlanningWorldData.get(world).operations.get(n.getUniqueId("PlanningOperation"));AttackOperation.Step step=operation==null?null:operation.step(n.getUniqueId("PlanningStep"));
        check("physical_planned_shell_"+shell.getUniqueID(),step!=null && shell.getAmmoType()==0 && shell.tubeArtilleryProfile()!=null,"correct payload, operation and step");
        flights.put(shell.getUniqueID(),shell);if(step!=null)targets.put(shell.getUniqueID(),new BlockPos(step.x,step.y,step.z));
    }
    private boolean complete(AttackOperation[] operations){for(AttackOperation o:operations)if(o==null || o.state!=AttackOperation.State.COMPLETE)return false;return true;}
    private boolean impacted(){for(EntityWarTechArtilleryProjectile shell:flights.values())if(!shell.isDead)return false;return true;}
    private void impacts(){for(Map.Entry<UUID,EntityWarTechArtilleryProjectile> row:flights.entrySet()){EntityWarTechArtilleryProjectile shell=row.getValue();BlockPos target=targets.get(row.getKey());check("physical_impact_"+row.getKey(),shell.isDead && target!=null && Math.hypot(shell.posX-(target.getX()+.5),shell.posZ-(target.getZ()+.5))<8,shell.getPositionVector());}}
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event)throws Exception{
        if(event.phase!=TickEvent.Phase.END)return;tick++;
        if(tick==20)setup();
        if(!resumed){
            if(tick==30){PlanningAssetRegistry.refresh(world);NBTTagCompound response=request("fetch",null);NBTTagList rows=response.getTagList("Assets",10);
                for(int i=0;i<2;i++){boolean found=false;for(int j=0;j<rows.tagCount();j++)if(rows.getCompoundTagAt(j).getString("Key").equals(PlanningAssetRegistry.key(guns[i]))){NBTTagCompound asset=rows.getCompoundTagAt(j);found=asset.getBoolean("Attack") && asset.getInteger("Ammo")==5 && asset.getInteger("Range")==guns[i].tubeProfile().range;}
                    check("attack_terminal_lists_"+i,found,"actual server snapshot");plans[i]=plan(i,2,false);
                    AttackOperation.Step bad=AttackOperation.Step.read(plans[i].steps.get(1).write());bad.x=(int)guns[i].posX+10;bad.z=(int)guns[i].posZ;check("minimum_range_gate_"+i,"minimum_range".equals(PlanningAssetAdapter.validate(world,plans[i],bad)),PlanningAssetAdapter.validate(world,plans[i],bad));
                    bad.x=(int)guns[i].posX+guns[i].tubeProfile().range+20;check("maximum_range_gate_"+i,"range".equals(PlanningAssetAdapter.validate(world,plans[i],bad)),PlanningAssetAdapter.validate(world,plans[i],bad));
                    check("wizard_preflight_"+i,OperationScheduler.validate(world,plans[i]).isEmpty(),plans[i].reason);
                    List<NBTTagCompound> remaining=AttackPlanBuilder.weapons(PlanningAssetRegistry.describe(guns[i]).write(),plans[i]);boolean reserved=false;for(NBTTagCompound weapon:remaining)if(weapon.getInteger("Slot")==2)reserved=weapon.getInteger("Available")==1;check("wizard_reserves_two_shells_"+i,reserved,remaining);
                }
            }
            if(tick==40)plans[0]=save(plans[0]);if(tick==45)plans[1]=save(plans[1]);if(tick==50)start(plans[0]);if(tick==55)start(plans[1]);
            if(stage==0 && tick>200 && complete(plans) && flights.size()==4 && impacted() && guns[0].reloadTicks()==0 && guns[1].reloadTicks()==0){
                impacts();for(int i=0;i<2;i++){check("two_native_releases_"+i,plans[i].steps.stream().filter(s->s.action==AttackOperation.Action.LAUNCH).allMatch(s->s.phase==AttackOperation.Phase.DONE && s.emitted==1 && s.flights.size()==1 && s.actual>=s.accepted),plans[i].write());
                    check("exact_selected_stock_and_energy_"+i,guns[i].getStackInSlot(0).getCount()==2 && guns[i].getStackInSlot(2).getCount()==1 && guns[i].getLegacyPower()==9000,guns[i].writeToNBT(new NBTTagCompound()));}
                stage=1;stageTick=tick;
            }
            if(stage==1){int delta=tick-stageTick;if(delta==5)cancelPlans[0]=save(plan(0,1,true));if(delta==10)cancelPlans[1]=save(plan(1,1,true));if(delta==15)start(cancelPlans[0]);if(delta==20)start(cancelPlans[1]);
                if(delta==30)for(int i=0;i<2;i++){check("pending_before_cancel_"+i,guns[i].isFiring(),guns[i].readiness());}
                if(delta==35){NBTTagCompound r=request("cancel",cancelPlans[0]);check("cancel_terminal_0",r!=null && r.getString("Error").isEmpty() && !guns[0].isFiring(),r==null?"null":r.getString("Error"));}
                if(delta==40){NBTTagCompound r=request("cancel",cancelPlans[1]);check("cancel_terminal_1",r!=null && r.getString("Error").isEmpty() && !guns[1].isFiring(),r==null?"null":r.getString("Error"));}
                if(delta==80){for(int i=0;i<2;i++)check("cancel_no_expenditure_"+i,guns[i].getStackInSlot(2).getCount()==1 && guns[i].getLegacyPower()==9000 && flights.size()==4,"unfired shell retained");stage=2;stageTick=tick;}
            }
            if(stage==2){int delta=tick-stageTick;if(delta==5)restartPlans[0]=save(plan(0,1,true));if(delta==10)restartPlans[1]=save(plan(1,1,true));if(delta==15)start(restartPlans[0]);if(delta==20)start(restartPlans[1]);
                if(delta==25){JsonObject saved=new JsonObject();JsonArray ids=new JsonArray(),operations=new JsonArray();for(int i=0;i<2;i++){check("pending_for_real_restart_"+i,guns[i].isFiring() && restartPlans[i].steps.get(1).phase==AttackOperation.Phase.ACCEPTED,restartPlans[i].write());ids.add(guns[i].getUniqueID().toString());operations.add(restartPlans[i].id.toString());}saved.add("guns",ids);saved.add("operations",operations);Files.write(CHECKPOINT,new Gson().toJson(saved).getBytes(StandardCharsets.UTF_8));finish();}
            }
        }else if(tick>100 && complete(restartPlans) && flights.size()==2 && impacted()){
            impacts();for(int i=0;i<2;i++){check("restart_one_release_"+i,restartPlans[i].steps.get(1).emitted==1 && restartPlans[i].steps.get(1).flights.size()==1,restartPlans[i].write());check("restart_exact_expenditure_"+i,guns[i].getStackInSlot(2).isEmpty() && guns[i].getStackInSlot(0).getCount()==2 && guns[i].getLegacyPower()==8500,guns[i].getLegacyPower());}finish();
        }
        if(tick%200==0)System.out.println("TUBEPLANNINGQA tick="+tick+" stage="+stage+" plans="+Arrays.toString(Arrays.stream(resumed?restartPlans:plans).map(o->o==null?"null":o.state+" "+o.steps.stream().map(s->s.phase+":"+s.reason+":"+s.emitted).collect(java.util.stream.Collectors.toList())).toArray()));
        if(tick==1600){check("bounded_completion",false,"terminal operation timeout");finish();}
    }
    private void finish()throws Exception{JsonObject r=new JsonObject();r.addProperty("complete",true);r.addProperty("resumed",resumed);r.add("cases",cases);r.add("failures",new Gson().toJsonTree(failures));Files.write(Paths.get(resumed?"tube-planning-restart-report.json":"tube-planning-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(r).getBytes(StandardCharsets.UTF_8));MinecraftForge.EVENT_BUS.unregister(this);FMLCommonHandler.instance().getMinecraftServerInstance().initiateShutdown();}
}
