package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Compact driving instruments plus telemetry appropriate to the vehicle's purpose. */
public final class VehicleHud {
    @SubscribeEvent public void onOverlay(RenderGameOverlayEvent.Post event) {
        Minecraft mc=Minecraft.getMinecraft();
        if(event.getType()!=RenderGameOverlayEvent.ElementType.ALL || mc.player==null || RemoteControlClient.isActive()
                || !(mc.player.getRidingEntity() instanceof EntityWarTechGroundVehicle) || mc.currentScreen!=null)return;
        render((EntityWarTechGroundVehicle)mc.player.getRidingEntity(),event.getResolution().getScaledWidth(),event.getResolution().getScaledHeight());
    }
    @SubscribeEvent public void onCrosshair(RenderGameOverlayEvent.Pre event) {
        if(event.getType()==RenderGameOverlayEvent.ElementType.CROSSHAIRS && Minecraft.getMinecraft().player!=null
                && Minecraft.getMinecraft().player.getRidingEntity() instanceof EntityWarTechGroundVehicle)event.setCanceled(true);
    }
    public static void render(EntityWarTechGroundVehicle v,int width,int height) {
        Minecraft mc=Minecraft.getMinecraft();int left=10,top=height-77;
        int panel=Math.min(width-20,345);
        Gui.drawRect(left,top,left+panel,top+38,0xBF101817);
        Gui.drawRect(left,top,left+3,top+38,0xFF6CAD9B);
        String direction=Math.abs(v.getDriveSpeed())<.003?"N":v.getDriveSpeed()>0?"D":"R";
        String first=String.format(java.util.Locale.ROOT,"%s   %4.1f %s   %s",name(v),Math.abs(v.getDriveSpeed())*20,tr("speed_unit"),direction);
        text(first,left+9,top+6,panel-16,0xE5EAE1);
        String brake=v.isParkingBrake()?tr("parking"):tr("driving");
        text(brake+"   "+tr("health",Math.round(v.getHealthValue()),Math.round(v.getHealthCapacity())),left+9,top+20,panel-16,0xA7BDB4);
        String hint=tr("controls",GameSettings.getKeyDisplayString(VehicleInputController.PANEL.getKeyCode()),
                GameSettings.getKeyDisplayString(VehicleInputController.PARK.getKeyCode()),
                GameSettings.getKeyDisplayString(mc.gameSettings.keyBindJump.getKeyCode()),
                GameSettings.getKeyDisplayString(mc.gameSettings.keyBindSneak.getKeyCode()));
        text(hint,left,top-14,width-20,0xD0D8CA);
        java.util.List<String> rows=new java.util.ArrayList<>();
        if(v instanceof EntityTubeArtillery){EntityTubeArtillery gun=(EntityTubeArtillery)v;rows.add(tr("shells",gun.ammunitionCount()));rows.add(I18n.format("wartec.tube."+gun.readiness()));
        }else if(v instanceof EntityModernVehicle) {
            EntityModernVehicle modern=(EntityModernVehicle)v;
            rows.add(tr("missiles",modern.loadedCount(),modern.modernProfile().slots));
            rows.add(I18n.format("wartec.modern.ui.status",(int)(modern.getDeployment()*100),modern.loadedCount(),modern.modernProfile().slots,modern.getQueuedShots()));
            if(!modern.modernProfile().isMlrs()) rows.add(I18n.format("wartec.modern.ui.contacts",modern.getLegacyContacts()));
        } else if(v instanceof EntityStrategicTel) {
            EntityStrategicTel tel=(EntityStrategicTel)v;
            rows.add(tr("missiles",Integer.bitCount(tel.getMissileMask()),tel.getTargetCount())+"  "+
                    tr(tel.getLaunchTicks()>0?"launching":tel.isDeployed()?tel.getErectionProgress()<100?"deploying":"ready":"stowed"));
            for(int i=0;i<tel.getTargetCount();i++) {
                net.minecraft.util.math.BlockPos target=tel.getTarget(i);
                rows.add(tr(tel.getTargetCount()==2?(i==0?"left":"right"):"target")+": "+
                        (tel.hasTarget(i)?target.getX()+" / "+target.getY()+" / "+target.getZ():tr("unset")));
            }
        } else if(v.getProfile()==WarTechEntityProfile.MOBILE_AIR_DEFENSE) {
            rows.add(tr("missiles",v.getDriveAmmo(),v.isTor()?8:12));
            rows.add(tr("radar")+": "+tr(v.isLegacyOperational()?"active":"standby")+"   IFF: "+v.getOwnerTeam());
            if(!v.isTor())rows.add(tr("gun_rounds",v.getPantsirGunRounds()));
        } else if(v.getProfile()==WarTechEntityProfile.MOBILE_ARTILLERY) {
            rows.add(tr("shells",v.getArtilleryAmmoCount()));
            rows.add(tr(v.isDeployed()?"ready":"stowed"));
        } else if(v.getProfile()==WarTechEntityProfile.COMMAND_TRUCK) {
            rows.add(tr("network")+": "+tr(v.isLegacyOperational()?"active":"standby"));
            rows.add(tr("power",(int)(100L*v.getLegacyPower()/Math.max(1,v.getEnergyCapacity()))));
        }
        int side=Math.min(260,width-20),sx=width-side-10;
        Gui.drawRect(sx,10,sx+side,20+rows.size()*13,0xA8101817);
        for(int i=0;i<rows.size();i++)text(rows.get(i),sx+7,16+i*13,side-14,0xD5E1D5);
    }
    public static String name(EntityWarTechGroundVehicle v) {
        if(v instanceof EntityTubeArtillery)return I18n.format("wartec.tube."+((EntityTubeArtillery)v).tubeProfile().model);
        if(v instanceof EntityModernVehicle) return tr(((EntityModernVehicle)v).modernProfile().model);
        String key=v instanceof EntityStrategicTel?((EntityStrategicTel)v).getStrategicSystem().getId():
            v.getProfile()==WarTechEntityProfile.MOBILE_AIR_DEFENSE?(v.isTor()?"tor":"pantsir"):
            v.getProfile()==WarTechEntityProfile.MOBILE_ARTILLERY?"artillery":"command";
        return tr(key);
    }
    private static String tr(String key,Object... args) {return I18n.format("wartecmod.vehicle."+key,args);}
    private static void text(String value,int x,int y,int max,int color) {
        net.minecraft.client.gui.FontRenderer f=Minecraft.getMinecraft().fontRenderer;
        if(f.getStringWidth(value)>max)value=f.trimStringToWidth(value,max-9)+"...";
        f.drawStringWithShadow(value,x,y,color);
    }
}
