package com.wartec.wartecmod.port.network;

import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.gui.*;
import com.wartec.wartecmod.port.gameplay.TileEntityMissileSilo;
import com.wartec.wartecmod.port.silo.HbmSiloHatch;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.*;

public final class SiloActionMessage implements IMessage {
    private int window,action,x,y,z;private long position;
    public SiloActionMessage(){}
    public SiloActionMessage(int window,BlockPos pos,int action,int x,int y,int z){this.window=window;position=pos.toLong();this.action=action;this.x=x;this.y=y;this.z=z;}
    @Override public void toBytes(ByteBuf b){b.writeInt(window).writeLong(position).writeInt(action).writeInt(x).writeInt(y).writeInt(z);}
    @Override public void fromBytes(ByteBuf b){window=b.readInt();position=b.readLong();action=b.readInt();x=b.readInt();y=b.readInt();z=b.readInt();}
    public static final class Handler implements IMessageHandler<SiloActionMessage,IMessage>{
        @Override public IMessage onMessage(SiloActionMessage m,MessageContext ctx){EntityPlayerMP p=ctx.getServerHandler().player;p.getServerWorld().addScheduledTask(()->{
            if(p.isSpectator() || !(p.openContainer instanceof ContainerMissileSilo) || p.openContainer.windowId!=m.window)return;
            ContainerMissileSilo c=(ContainerMissileSilo)p.openContainer;if(!c.canInteractWith(p) || c.source.getPos().toLong()!=m.position)return;
            TileEntityMissileSilo t=c.tile;
            switch(m.action){
                case 0:t.status=t.checkSite();if(t.status.isEmpty())t.status="silo.state.site_ok";t.changed();break;
                case 1:t.build(p);break;
                case 2:if(Math.abs((long)m.x)<29999984 && Math.abs((long)m.z)<29999984)t.program(p,new BlockPos(m.x,m.y,m.z));break;
                case 3:t.importTarget(p,c);break;
                case 4:t.beginLaunch(p);break;
                case 5:if(!t.locked() && !t.isLaunchReady() && t.findHatch())HbmSiloHatch.open(t.getWorld(),t.hatchPos);break;
                case 6:t.closeHatch(p);break;
                case 7:p.openGui(WarTechReforged.instance,WarTechGuiHandler.GUI_SILO,p.world,c.source.getPos().getX(),c.source.getPos().getY(),c.source.getPos().getZ());break;
                case 8:c.source.renovate(p);break;
                case 9:t.setLaunchReady(p,!t.isLaunchReady());break;
                case 10:t.setDefenseEnabled(p,!t.isDefenseEnabled());break;
                default:break;
            }
        });return null;}
    }
}
