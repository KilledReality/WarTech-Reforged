package com.wartec.flightqa;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.TileEntityCruiseLauncher;
import com.wartec.wartecmod.port.gui.ContainerCruiseLauncher;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.*;

/** Actual server container click/quick-move/hopper paths; never ships in production. */
final class GroundLauncherQa {
    static JsonArray run(WorldServer world,List<String> failures) {
        JsonArray rows=new JsonArray();BlockPos pos=new BlockPos(48,64,0);
        FakePlayer player=FakePlayerFactory.get(world,new GameProfile(new UUID(7171,10),"LauncherQA"));
        FakePlayer stranger=FakePlayerFactory.get(world,new GameProfile(new UUID(7171,11),"OtherQA"));
        player.setPosition(48.5,64,.5);stranger.setPosition(48.5,64,.5);
        for(CruisePartDefinition body:CruiseAirframes.bodies()) for(int mode=0;mode<2;mode++) {
            for(int i=0;i<36;i++) player.inventory.setInventorySlotContents(i,ItemStack.EMPTY);
            player.inventory.setItemStack(ItemStack.EMPTY);
            CruiseLaunchPointBlock block=body==CruisePartDefinition.BODY_LIGHT?WarTechContent.CRUISE_DRONE_RAIL:WarTechContent.CRUISE_LAUNCH_POINT;
            world.setBlockToAir(pos);world.setBlockState(pos,block.getDefaultState().withProperty(CruiseLaunchPointBlock.FACING,EnumFacing.SOUTH),3);
            TileEntityCruiseLauncher tile=(TileEntityCruiseLauncher)world.getTileEntity(pos);
            CruiseBuild b=CruiseBuild.starter(body);CruiseMission mission=new CruiseMission();mission.setTarget(new Vec3d(48,4,400),0);
            ItemStack stack=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);b.writeToStack(stack);mission.writeToStack(stack);
            NBTTagCompound expected=stack.getTagCompound().copy();
            ContainerCruiseLauncher c=new ContainerCruiseLauncher(player.inventory,tile);
            if(!c.inventorySlots.get(0).isItemValid(stack) || !tile.isItemValidForSlot(0,stack)) failures.add("Valid missile rejected "+body);
            if(mode==0) { player.inventory.setItemStack(stack.copy());c.slotClick(0,0,ClickType.PICKUP,player); }
            else { player.inventory.setInventorySlotContents(9,stack.copy());c.transferStackInSlot(player,1); }
            if(tile.isEmpty() || !expected.equals(tile.getStackInSlot(0).getTagCompound())) failures.add("Insertion lost missile NBT "+body+"/"+mode);
            UUID first=tile.getLoadId();
            if(first==null || tile.mayUse(stranger) || c.inventorySlots.get(0).canTakeStack(stranger)) failures.add("Ownership or load ID lost "+body);
            CruiseBuild air=CruiseBuild.starter(body);air.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);
            ItemStack bad=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);air.writeToStack(bad);
            if(c.inventorySlots.get(0).isItemValid(bad)) failures.add("Air adapter accepted on ground");
            c.transferStackInSlot(player,0);
            if(!tile.isEmpty() || tile.getLoadId()!=null) failures.add("Extraction did not invalidate chain link");
            player.inventory.setInventorySlotContents(9,stack.copy());c.transferStackInSlot(player,1);
            if(first.equals(tile.getLoadId())) failures.add("Reload reused stale chain link");
            int before=world.loadedEntityList.size();boolean launch=tile.launch(player);
            if(!launch || !tile.isEmpty() || tile.getLoadId()!=null) failures.add("Ground button launch failed "+body+"/"+mode);
            int spawned=0;
            for(Entity e:new ArrayList<Entity>(world.loadedEntityList)) if(e instanceof EntityCustomCruise && !e.isDead) {
                spawned++;if(!player.getUniqueID().equals(((EntityCustomCruise)e).getOwnerUuid())) failures.add("Launch owner lost");e.setDead();
            }
            if(spawned!=1) failures.add("Wrong missile count "+spawned);
            JsonObject row=new JsonObject();row.addProperty("body",body.getId());row.addProperty("insertion",mode==0?"click":"shift_click");
            row.addProperty("launched",launch);row.addProperty("spawned",spawned);rows.add(row);
        }
        world.setBlockToAir(pos);world.setBlockState(pos,WarTechContent.CRUISE_LAUNCH_POINT.getDefaultState(),3);
        TileEntityCruiseLauncher hopper=(TileEntityCruiseLauncher)world.getTileEntity(pos);
        ItemStack valid=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC).writeToStack(valid);
        if(!TileEntityHopper.putStackInInventoryAllSlots(null,hopper,valid.copy(),EnumFacing.UP).isEmpty() || hopper.isEmpty()) failures.add("Hopper insertion failed");
        hopper.clear();
        if(hopper.isItemValidForSlot(0,new ItemStack(WarTechContent.ASSEMBLED_CRUISE)) || hopper.isItemValidForSlot(1,valid)) failures.add("Invalid build/slot accepted");
        world.setBlockToAir(pos);return rows;
    }
}
