package com.wartec.flightqa;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.*;
import com.hbm.tileentity.machine.*;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.*;
import com.wartec.wartecmod.port.integration.NetworkTeamHelper;
import com.wartec.wartecmod.port.production.*;
import com.wartec.wartecmod.port.silo.*;
import com.wartec.wartecmod.port.uav.UavReconReport;
import java.util.*;
import net.minecraft.init.*;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

/** Stateful native regressions for resources which must never regenerate for free. */
final class ProductionConservationQa {
    private ProductionQa qa;private WorldServer world;private FakePlayer player;
    void run(WorldServer w,ProductionQa q) throws Exception {
        world=w;qa=q;player=new FakePlayer(w,new GameProfile(new UUID(112,1),"ProductionQA"));NetworkTeamHelper.setPlayerTeam(player,"production-blue");
        press();blast();display();silo();recon();upgrade();
    }
    private ItemStack representative(AStack a){ItemStack s=a.getStackList().get(0).copy();s.setCount(a.count());return s;}
    private void press(){
        TileEntityMachinePress tile=new TileEntityMachinePress();tile.setWorld(world);tile.setPos(new BlockPos(80,8,80));
        ItemStack ingot=representative(ProductionRecipes.ingredients(ProductionCatalog.recipe("wartecmod:itemplateu238@0"))[0]);
        ItemStack stamp=com.hbm.items.machine.ItemStamp.stamps.get(com.hbm.items.machine.ItemStamp.StampType.PLATE).get(0).copy();
        tile.inventory.setStackInSlot(1,stamp);tile.inventory.setStackInSlot(2,ingot);tile.burnTime=2000;
        qa.check("u238_press_match",PressRecipes.getOutput(ingot,tile.inventory.getStackInSlot(1)).getItem()==WarTechContent.ITEM_PLATE_U238,"PLATE stamp");
        for(int i=0;i<1000 && tile.inventory.getStackInSlot(3).isEmpty();i++)((ITickable)tile).update();
        qa.check("u238_press_product",tile.inventory.getStackInSlot(3).getItem()==WarTechContent.ITEM_PLATE_U238 && tile.inventory.getStackInSlot(3).getCount()==1,"one ingot, one plate");
        qa.check("u238_press_exact_input",tile.inventory.getStackInSlot(2).isEmpty() && tile.burnTime==1800,tile.burnTime);
        qa.check("u238_press_stamp_wear",!stamp.isItemStackDamageable() || tile.inventory.getStackInSlot(1).getItemDamage()==1,"normal HBM stamp wear");
    }
    private void blast(){
        TileEntityMachineBlastFurnace tile=new TileEntityMachineBlastFurnace();tile.setWorld(world);tile.setPos(new BlockPos(80,8,96));
        AStack[] inputs=ProductionRecipes.ingredients(ProductionCatalog.recipe("wartecmod:itemingotarmorsteel@0"));tile.inventory.setStackInSlot(1,representative(inputs[0]));tile.inventory.setStackInSlot(2,representative(inputs[1]));
        ((ITickable)tile).update();qa.check("armor_blast_no_heat",tile.inventory.getStackInSlot(3).isEmpty(),"no free smelting");
        tile.fuel=800;tile.muffled=true;
        for(int i=0;i<2500 && tile.inventory.getStackInSlot(3).isEmpty();i++)((ITickable)tile).update();
        qa.check("armor_blast_product",tile.inventory.getStackInSlot(3).getItem()==WarTechContent.ITEM_INGOT_ARMOR_STEEL && tile.inventory.getStackInSlot(3).getCount()==4,tile.inventory.getStackInSlot(3));
        qa.check("armor_blast_exact_inputs_heat",tile.inventory.getStackInSlot(1).isEmpty() && tile.inventory.getStackInSlot(2).isEmpty() && tile.fuel==0,tile.fuel);
    }
    private void display(){
        BlockPos p=new BlockPos(96,8,96);world.getChunkFromBlockCoords(p);LegacyDecorationBlock block=(LegacyDecorationBlock)WarTechContent.DECO_BLOCK_CRUISE_MISSILE;
        ItemStack source=new ItemStack(WarTechContent.ITEM_CRUISE_MISSILE_HE);NBTTagCompound payload=new NBTTagCompound();payload.setLong("UsedEnergy",4567);source.setTagCompound(payload);
        ItemStack wrapped=new ItemStack(block);NBTTagCompound tag=new NBTTagCompound();tag.setTag(PreservingRecipe.PAYLOAD,source.serializeNBT());wrapped.setTagCompound(tag);
        world.setBlockState(p,block.getDefaultState(),3);block.onBlockPlacedBy(world,p,block.getDefaultState(),player,wrapped);
        TileEntityWarTechVisual tile=(TileEntityWarTechVisual)world.getTileEntity(p);NBTTagCompound saved=tile.writeToNBT(new NBTTagCompound());TileEntityWarTechVisual restored=new TileEntityWarTechVisual();restored.readFromNBT(saved);world.setTileEntity(p,restored);
        NonNullList<ItemStack> drops=NonNullList.create();block.getDrops(drops,world,p,world.getBlockState(p),0);
        qa.check("display_place_save_break_preserves_payload",drops.size()==1 && ItemStack.areItemStacksEqual(source,new ItemStack(drops.get(0).getTagCompound().getCompoundTag(PreservingRecipe.PAYLOAD))),drops);
        world.setBlockToAir(p);
    }
    private void silo(){
        BlockPos anchor=new BlockPos(1032,80,1032),surface=SiloGeometry.at(anchor,EnumFacing.SOUTH,0,0,6);
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)world.getChunkFromChunkCoords((anchor.getX()>>4)+x,(anchor.getZ()>>4)+z);
        player.setPosition(surface.getX()+.5,surface.getY()+1,surface.getZ()+.5);world.setBlockState(surface,WarTechContent.MISSILE_SILO.getDefaultState(),3);
        TileEntityMissileSilo site=(TileEntityMissileSilo)world.getTileEntity(surface);site.claim(player);site.facing=EnumFacing.SOUTH;site.buildCredits=0;
        qa.check("silo_unpaid_build_denied",!site.build(player) && site.buildIndex<0,"no free autostructure");
        ItemStack fresh=ProductionCatalog.output(ProductionCatalog.recipe("wartecmod:missilesilo@0"));WarTechContent.MISSILE_SILO.onBlockPlacedBy(world,surface,world.getBlockState(surface),player,fresh);site.facing=EnumFacing.SOUTH;
        qa.check("silo_paid_build_accepts",site.build(player) && site.buildCredits==0,"one-use credit consumed");
        NBTTagCompound inProgress=site.writeToNBT(new NBTTagCompound());site.readFromNBT(inProgress);qa.check("silo_credit_survives_save",site.buildCredits==0 && site.buildIndex>=0,site.buildCredits);
        finish(site);BlockPos panel=SiloGeometry.at(anchor,EnumFacing.SOUTH,0,-4,6);TileEntityMissileSilo controller=(TileEntityMissileSilo)world.getTileEntity(panel);
        qa.check("silo_built_moves_consumed_credit",controller!=null && controller.builtStructure && controller.buildCredits==0,controller==null?"missing":controller.buildCredits);
        if(controller==null)return;
        player.setPosition(panel.getX()+.5,panel.getY()+1,panel.getZ()+.5);
        qa.check("silo_intact_renovation_reuses_paid_parts",controller.renovate(player),controller.status);finish(controller);controller=(TileEntityMissileSilo)world.getTileEntity(panel);
        BlockPos missing=null;for(SiloStructure.Cell c:SiloStructure.cells(EnumFacing.SOUTH))if(c.state.getBlock()==com.hbm.blocks.ModBlocks.concrete_rebar){missing=SiloGeometry.at(anchor,EnumFacing.SOUTH,c.x,c.y,c.z);break;}
        if(missing==null)throw new IllegalStateException("No concrete cell");world.setBlockToAir(missing);
        qa.check("silo_harvest_restore_denied",!controller.renovate(player),controller.status);
        player.inventory.setInventorySlotContents(0,ProductionCatalog.stack("plan:silo_build_kit",1));
        qa.check("silo_replacement_kit_consumed",controller.renovate(player) && player.inventory.getStackInSlot(0).isEmpty(),controller.status);finish(controller);
        qa.check("silo_paid_repair_restores_missing_block",world.getBlockState(missing).getBlock()==com.hbm.blocks.ModBlocks.concrete_rebar,missing);
        BlockPos spent=new BlockPos(1120,80,1120);world.getChunkFromBlockCoords(spent);world.setBlockState(spent,WarTechContent.MISSILE_SILO.getDefaultState(),3);
        TileEntityMissileSilo used=(TileEntityMissileSilo)world.getTileEntity(spent);used.buildCredits=0;NonNullList<ItemStack> drops=NonNullList.create();WarTechContent.MISSILE_SILO.getDrops(drops,world,spent,world.getBlockState(spent),0);
        qa.check("silo_unbuilt_drop_keeps_spent_state",drops.size()==1 && drops.get(0).getTagCompound().getInteger("WarTechBuildCredits")==0,drops);
        WarTechContent.MISSILE_SILO.onBlockPlacedBy(world,spent,world.getBlockState(spent),player,drops.get(0));qa.check("silo_replace_no_new_credit",used.buildCredits==0,used.buildCredits);
    }
    private void finish(TileEntityMissileSilo site){int n=0;while(site.buildIndex>=0 && n++<10000)site.update();qa.check("silo_build_completion_"+site.getPos(),site.buildIndex<0,n+" updates; "+site.status);}
    private void recon() throws Exception {
        EntityCustomUav uav=new EntityCustomUav(world);java.lang.reflect.Field field=EntityCustomUav.class.getDeclaredField("reconReport");field.setAccessible(true);UavReconReport report=(UavReconReport)field.get(uav);report.beginDimension(0);report.recordCell(1,1,64,0x668844,1);
        player.inventory.clear();uav.sendFleetReconReport(player);qa.check("recon_no_paper_no_report",countReports()==0,"observations cannot fabricate a paper item");
        player.inventory.setInventorySlotContents(0,new ItemStack(Items.PAPER,2));uav.sendFleetReconReport(player);
        qa.check("recon_actual_observation_and_paper",countReports()==1 && player.inventory.getStackInSlot(0).getCount()==1,countReports());
        uav.sendFleetReconReport(player);int paper=0;for(int i=0;i<player.inventory.getSizeInventory();i++)if(player.inventory.getStackInSlot(i).getItem()==Items.PAPER)paper+=player.inventory.getStackInSlot(i).getCount();
        qa.check("recon_each_download_costs_paper",countReports()==2 && paper==0,countReports());player.inventory.clear();
    }
    private int countReports(){int n=0;for(int i=0;i<player.inventory.getSizeInventory();i++){ItemStack s=player.inventory.getStackInSlot(i);if(s.getItem()==WarTechContent.UAV_RECON_REPORT)n+=s.getCount();}return n;}
    private void upgrade(){
        ItemStack fresh=ProductionCatalog.output(ProductionCatalog.recipe("wartecmod:mobileartillery@0"));UnusedVehicleIngredient ingredient=new UnusedVehicleIngredient(new ItemStack(WarTechContent.MOBILE_ARTILLERY));
        qa.check("vehicle_upgrade_factory_allowed",ingredient.matchesRecipe(fresh,true),"unused chassis");
        fresh.getTagCompound().setInteger("WarTechRecoveredPower",123);fresh.getTagCompound().setFloat("WarTechRecoveredHealth",10);
        qa.check("vehicle_upgrade_used_chassis_denied",!ingredient.matchesRecipe(fresh,true),"cannot erase stored power/damage");
        EntityWarTechGroundVehicle henry=new EntityWarTechGroundVehicle(world,WarTechEntityProfile.MOBILE_ARTILLERY);henry.setVisual(henry.getVisualId(),2);NBTTagCompound n=new NBTTagCompound();n.setInteger("ArtilleryAmmoType",0);n.setInteger("ArtilleryAmmoCount",3);henry.restoreProductionChassis(n);
        qa.check("henry_partial_loaded_pack_preserved",henry.getArtilleryAmmoCount()==3 && henry.productionChassis().getInteger("ArtilleryAmmoCount")==3,henry.getArtilleryAmmoCount());
    }
}
