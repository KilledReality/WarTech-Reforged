package com.wartec.wartecmod.port.content;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public final class MissileItem extends PortIntentItem
        implements com.wartec.wartecmod.items.IMissileSpawningItem {
    private final MissileProfile profile;

    public MissileItem(String legacyRegistryName, CreativeTabs tab, MissileProfile profile) {
        super(
            legacyRegistryName,
            tab,
            isMlrs(profile) ? 16 : 1,
            IntentKind.MISSILE,
            "missile/" + profile.getIntentPath(),
            isMlrs(profile) ? new String[]{"he","phosphorus","cluster","thermobaric","fragmentation"} : new String[]{"default"}
        );
        this.profile = profile;
    }

    private static boolean isMlrs(MissileProfile profile) {
        return profile==MissileProfile.GRAD_ROCKET || profile==MissileProfile.URAGAN_ROCKET
            || profile==MissileProfile.SMERCH_ROCKET || profile==MissileProfile.HIMARS_ROCKET;
    }

    public MissileProfile getProfile() {
        return profile;
    }

    @Override public net.minecraft.util.ActionResult<ItemStack> onItemRightClick(World world,net.minecraft.entity.player.EntityPlayer player,net.minecraft.util.EnumHand hand) {
        if(profile!=MissileProfile.S8000) return super.onItemRightClick(world,player,hand);
        if(!world.isRemote) player.openGui(com.wartec.wartecmod.WarTechReforged.instance,
            com.wartec.wartecmod.port.gui.WarTechGuiHandler.GUI_CRUISE_PROGRAMMER,world,hand==net.minecraft.util.EnumHand.OFF_HAND?40:player.inventory.currentItem,0,0);
        return new net.minecraft.util.ActionResult<>(net.minecraft.util.EnumActionResult.SUCCESS,player.getHeldItem(hand));
    }

    @Override public net.minecraft.util.EnumActionResult onItemUse(net.minecraft.entity.player.EntityPlayer player,World world,
            net.minecraft.util.math.BlockPos pos,net.minecraft.util.EnumHand hand,net.minecraft.util.EnumFacing face,float x,float y,float z) {
        if(profile==MissileProfile.S8000 && world.getTileEntity(pos) instanceof com.wartec.wartecmod.port.gameplay.TileEntityCruiseLauncher) {
            if(world.isRemote) return net.minecraft.util.EnumActionResult.SUCCESS;
            return player.canPlayerEdit(pos,face,player.getHeldItem(hand)) && world.isBlockModifiable(player,pos)
                && ((com.wartec.wartecmod.port.gameplay.TileEntityCruiseLauncher)world.getTileEntity(pos)).load(player,player.getHeldItem(hand))
                ?net.minecraft.util.EnumActionResult.SUCCESS:net.minecraft.util.EnumActionResult.FAIL;
        }
        return super.onItemUse(player,world,pos,hand,face,x,y,z);
    }

    @Override
    public Class<? extends net.minecraft.entity.Entity> getMissile() {
        return com.wartec.wartecmod.port.entity.EntityWarTechMissile.class;
    }

    @Override
    public net.minecraft.nbt.NBTTagCompound createIntentTag(ItemStack stack) {
        net.minecraft.nbt.NBTTagCompound intent = super.createIntentTag(stack);
        intent.setString("FlightClass", profile.getFlightClass().name().toLowerCase(java.util.Locale.ROOT));
        intent.setString("PayloadClass", profile.getPayloadClass().name().toLowerCase(java.util.Locale.ROOT));
        return intent;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
            List<String> tooltip, ITooltipFlag flag) {
        double range=com.wartec.wartecmod.port.integration.WeaponBalance.missileRange(profile);
        if(range>0) tooltip.add(net.minecraft.client.resources.I18n.format("flight.tooltip.range",(int)range));
        com.wartec.wartecmod.port.entity.ModernMissileProfile modern=com.wartec.wartecmod.port.entity.ModernMissileProfile.of(profile);
        if(modern!=null) {
            if(modern.isMlrs()) {
                tooltip.add(net.minecraft.client.resources.I18n.format("wartec.modern.payload."+getVariantName(stack)+".description"));
                tooltip.add(net.minecraft.client.resources.I18n.format("wartec.modern.missile.stack"));
            } else tooltip.add(net.minecraft.client.resources.I18n.format("wartec.modern.missile.stats",modern.strength,modern.speed*20));
            tooltip.add(net.minecraft.client.resources.I18n.format("wartec.modern.missile.launch."+profile.getIntentPath()));return;
        }
        switch (profile) {
            case GERAN_5:
                for(String key:new String[]{"speed","warhead","launch","defense","control"})
                    tooltip.add(net.minecraft.client.resources.I18n.format("geran5.tooltip."+key));
                return;
            case ANTI_AIR_TIER_1:
                tooltip.add(TextFormatting.GOLD + "Can be used against:");
                tooltip.add(TextFormatting.YELLOW + " Cruise Missiles");
                return;
            case ANTI_AIR_TIER_2:
                tooltip.add(TextFormatting.GOLD + "Can be used against:");
                tooltip.add(TextFormatting.YELLOW + " Ballistic Missiles");
                return;
            case ANTI_AIR_TIER_3:
                tooltip.add(TextFormatting.GOLD + "Can be used against:");
                tooltip.add(TextFormatting.YELLOW + " Cruise Missiles");
                tooltip.add(TextFormatting.YELLOW + " Ballistic Missiles");
                return;
            case KH555:
                tooltip.add("Tier 2 long-range cruise missile | HE warhead");
                tooltip.add("Air release: 250-2,000 blocks | strategic-aircraft compatible");
                tooltip.add("Compatible with the standard WarTech launch tube");
                return;
            case ANTI_RADIATION:
                tooltip.add("Seeker radius: 1,200 blocks around the designated point");
                tooltip.add("Tracks active radars, radar decoys, and hostile jammers");
                tooltip.add("Remembers the last emitter position after radar shutdown");
                return;
            case TOMAHAWK:
                namedCruise(tooltip, "10.0", "6.25m");
                return;
            case KALIBR:
                namedCruise(tooltip, "10.0", "7.20m");
                return;
            case CJ10:
                namedCruise(tooltip, "10.0", "7.30m");
                return;
            case ISKANDER:
                tooltip.add(TextFormatting.BOLD + "Warhead: "
                        + TextFormatting.YELLOW + "HE");
                tooltip.add(TextFormatting.BOLD + "Strength: "
                        + TextFormatting.GRAY + com.wartec.wartecmod.port.integration.WeaponBalance.ISKANDER_BLAST_STRENGTH);
                tooltip.add(TextFormatting.BOLD + "Size: "
                        + TextFormatting.GRAY + "7.30m");
                return;
            case SUPERSONIC_HE:
            case SUPERSONIC_HYDROGEN:
                flightDescription(tooltip, "Supersonic", TextFormatting.YELLOW,
                        "Ramjet", "60.0cm", "2000", "< 5 Blocks", "7.0HP");
                return;
            case HYPERSONIC_HE:
            case HYPERSONIC_NUCLEAR:
                flightDescription(tooltip, "Hypersonic", TextFormatting.GREEN,
                        "Scramjet", "60.0cm", "1250", "< 7 Blocks", "5.0HP");
                return;
            case CRUISE_HE:
            case CRUISE_CLUSTER:
            case CRUISE_BUSTER:
            case CRUISE_EMP:
            case CRUISE_THERMOBARIC:
            case CRUISE_NUCLEAR:
            case CRUISE_HYDROGEN:
                flightDescription(tooltip, "Subsonic", TextFormatting.RED,
                        "Turbojet", "51.8cm", "3500", "< 3 Blocks", "10.0HP");
                return;
            default:
                break;
        }
    }

    private static void namedCruise(List<String> tooltip, String strength,
            String length) {
        tooltip.add(TextFormatting.BOLD + "Warhead: "
                + TextFormatting.YELLOW + "HE");
        tooltip.add(TextFormatting.BOLD + "Strength: "
                + TextFormatting.GRAY + strength);
        tooltip.add(TextFormatting.BOLD + "Size: "
                + TextFormatting.GRAY + length);
        tooltip.add(TextFormatting.BOLD + "Speed: "
                + TextFormatting.RED + "Subsonic");
        tooltip.add(TextFormatting.BOLD + "Propulsion: "
                + TextFormatting.RED + "Turbojet");
        tooltip.add(TextFormatting.BOLD + "Size: "
                + TextFormatting.GRAY + "51.8cm");
        tooltip.add(TextFormatting.BOLD + "Min. Range: "
                + TextFormatting.GRAY + "250 Blocks");
        tooltip.add(TextFormatting.BOLD + "Max. Range: "
                + TextFormatting.GREEN + "3500 Blocks");
        tooltip.add(TextFormatting.BOLD + "Health: "
                + TextFormatting.GREEN + "10.0HP");
    }

    private static void flightDescription(List<String> tooltip, String speed,
            TextFormatting color, String propulsion, String size,
            String maximumRange, String inaccuracy, String health) {
        tooltip.add(TextFormatting.BOLD + "Speed: " + color + speed);
        tooltip.add(TextFormatting.BOLD + "Propulsion: " + color + propulsion);
        tooltip.add(TextFormatting.BOLD + "Size: "
                + TextFormatting.GRAY + size);
        tooltip.add(TextFormatting.BOLD + "Min. Range: "
                + TextFormatting.GRAY + "250 Blocks");
        tooltip.add(TextFormatting.BOLD + "Max. Range: " + color
                + maximumRange + " Blocks");
        tooltip.add(TextFormatting.BOLD + "Inaccuracy: " + color + inaccuracy);
        tooltip.add(TextFormatting.BOLD + "Health: " + color + health);
    }
}
