package com.wartec.wartecmod.port.content;

import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.EntityCustomCruise;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public final class AssembledCruiseItem extends PortItem {
    public AssembledCruiseItem(String name,CreativeTabs tab) { super(name,tab,1); }
    @Override public String getItemStackDisplayName(ItemStack stack) { return CruiseBuild.fromStack(stack).getName(); }
    @Override public void getSubItems(CreativeTabs tab,NonNullList<ItemStack> items) {
        if(!isInCreativeTab(tab)) return;
        for(CruisePartDefinition body:CruiseAirframes.bodies()) {
            CruiseBuild build=CruiseBuild.starter(body);
            build.setName(net.minecraft.util.text.translation.I18n.translateToLocal("cruise.starter."+body.getId()));
            ItemStack ground=new ItemStack(this);build.writeToStack(ground);items.add(ground);
            build.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);
            build.setName(net.minecraft.util.text.translation.I18n.translateToLocal("cruise.starter."+body.getId())+" / "+CruiseText.text("air_short"));
            ItemStack air=new ItemStack(this);build.writeToStack(air);items.add(air);
        }
    }
    @Override public ActionResult<ItemStack> onItemRightClick(World world,EntityPlayer player,EnumHand hand) {
        if(!world.isRemote) player.openGui(WarTechReforged.instance,com.wartec.wartecmod.port.gui.WarTechGuiHandler.GUI_CRUISE_PROGRAMMER,world,hand==EnumHand.OFF_HAND?40:player.inventory.currentItem,0,0);
        return new ActionResult<>(EnumActionResult.SUCCESS,player.getHeldItem(hand));
    }
    @Override public EnumActionResult onItemUse(EntityPlayer player,World world,BlockPos pos,EnumHand hand,EnumFacing face,float x,float y,float z) {
        IBlockState state=world.getBlockState(pos);
        if(!(state.getBlock() instanceof CruiseLaunchPointBlock)) {
            if(!world.isRemote) player.openGui(WarTechReforged.instance,com.wartec.wartecmod.port.gui.WarTechGuiHandler.GUI_CRUISE_PROGRAMMER,world,hand==EnumHand.OFF_HAND?40:player.inventory.currentItem,0,0);
            return EnumActionResult.SUCCESS;
        }
        if(world.isRemote) return EnumActionResult.SUCCESS;
        if(!player.canPlayerEdit(pos,face,player.getHeldItem(hand)) || !world.isBlockModifiable(player,pos)) return EnumActionResult.FAIL;
        net.minecraft.tileentity.TileEntity tile=world.getTileEntity(pos);
        return tile instanceof com.wartec.wartecmod.port.gameplay.TileEntityCruiseLauncher
            && ((com.wartec.wartecmod.port.gameplay.TileEntityCruiseLauncher)tile).load(player,player.getHeldItem(hand))?EnumActionResult.SUCCESS:EnumActionResult.FAIL;
    }
    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    @Override public void addInformation(ItemStack stack,@Nullable World world,List<String> tooltip,ITooltipFlag flag) {
        CruiseBuild build=CruiseBuild.fromStack(stack);CruiseStats stats=build.calculateStats();
        tooltip.add(CruiseText.text("stats.range",stats.getRange()));
        tooltip.add(CruiseText.text("stats.speed",String.format(Locale.US,"%.1f",stats.getSpeed()*20)));
        tooltip.add(CruiseText.text("stats.mass",String.format(Locale.US,"%.0f",stats.getMass()),String.format(Locale.US,"%.0f",stats.getMaximumMass())));
        tooltip.add(CruiseText.text("stats.tiers",CruiseCombatProfile.navigationTier(build),CruiseCombatProfile.threatTier(build)));
        tooltip.add(CruiseText.text("stats.cep",String.format(Locale.US,"%.1f",CruiseCombatProfile.cep(build,0,false))));
        if(build.get(CruiseSlot.SEEKER)!=null && build.get(CruiseSlot.SEEKER)!=CruisePartDefinition.SEEKER_NONE)
            tooltip.add(CruiseText.text("stats.cep_locked",String.format(Locale.US,"%.1f",CruiseCombatProfile.cep(build,0,true))));
        tooltip.add(CruiseText.text("stats.cep_note"));
        for(CruiseSlot slot:new CruiseSlot[]{CruiseSlot.BODY,CruiseSlot.WARHEAD,CruiseSlot.LAUNCH})
            if(build.get(slot)!=null) tooltip.add(build.get(slot).getDisplayName());
        CruiseMission mission=CruiseMission.fromStack(stack);
        if(world!=null && world.isRemote && net.minecraft.client.Minecraft.getMinecraft().player!=null && !mission.getTargets().isEmpty()) {
            double length=mission.routeLength(net.minecraft.client.Minecraft.getMinecraft().player.getPositionVector());
            tooltip.add(CruiseText.text("stats.mission_cep",String.format(Locale.US,"%.1f",CruiseCombatProfile.cep(build,length,false))));
        }
        tooltip.add(CruiseText.text("mode."+mission.getMode().name().toLowerCase(Locale.ROOT)));
        if(mission.getMode()==CruiseMission.Mode.SEARCH) tooltip.add(CruiseText.text("program.category",CruiseText.text("category."+mission.getCategory().name().toLowerCase(Locale.ROOT))));
        tooltip.add(CruiseText.text("program.count",mission.getTargets().size(),mission.getMode()==CruiseMission.Mode.SEARCH?8:1));
        tooltip.add(CruiseText.text("item.instructions"));
        if(!stats.isValid()) tooltip.add(CruiseText.text("build_error."+stats.getErrors().get(0)));
    }
}
