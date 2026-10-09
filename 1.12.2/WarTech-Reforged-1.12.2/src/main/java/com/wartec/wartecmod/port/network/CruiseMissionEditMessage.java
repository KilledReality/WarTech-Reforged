package com.wartec.wartecmod.port.network;

import com.wartec.wartecmod.port.content.AssembledCruiseItem;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.integration.DesignatorCompat;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.network.simpleimpl.*;

/** Edits only the currently held missile, on the server thread. No client NBT. */
public final class CruiseMissionEditMessage implements IMessage {
    private int slot,checksum,action,entityId=-1;
    private double x,y,z;
    public CruiseMissionEditMessage() { }
    public CruiseMissionEditMessage(int slot,int checksum,int action,double x,double y,double z) {
        this.slot=slot;this.checksum=checksum;this.action=action;this.x=x;this.y=y;this.z=z;
    }
    public CruiseMissionEditMessage(int slot,int checksum,int entityId) { this(slot,checksum,4,0,0,0);this.entityId=entityId; }
    @Override public void fromBytes(ByteBuf b) { slot=b.readUnsignedByte();checksum=b.readInt();action=b.readUnsignedByte();x=b.readDouble();y=b.readDouble();z=b.readDouble();entityId=b.readInt(); }
    @Override public void toBytes(ByteBuf b) { b.writeByte(slot);b.writeInt(checksum);b.writeByte(action);b.writeDouble(x);b.writeDouble(y);b.writeDouble(z);b.writeInt(entityId); }
    public static final class Handler implements IMessageHandler<CruiseMissionEditMessage,IMessage> {
        @Override public IMessage onMessage(CruiseMissionEditMessage m,MessageContext ctx) {
            EntityPlayerMP player=ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(()->{
                if((m.slot!=player.inventory.currentItem && m.slot!=40) || m.action>6) return;
                if(!(player.openContainer instanceof com.wartec.wartecmod.port.gui.ContainerCruiseProgrammer)) return;
                com.wartec.wartecmod.port.gui.ContainerCruiseProgrammer container=(com.wartec.wartecmod.port.gui.ContainerCruiseProgrammer)player.openContainer;
                if(container.getMissileSlot()!=m.slot || !container.canInteractWith(player)) return;
                ItemStack stack=player.inventory.getStackInSlot(m.slot);
                if(stack.isEmpty() || !(stack.getItem() instanceof AssembledCruiseItem)) return;
                CruiseBuild build=CruiseBuild.fromStack(stack);
                if(build.checksum()!=m.checksum || !build.calculateStats().isValid()) return;
                CruiseMission mission=CruiseMission.fromStack(stack);
                if(m.action==4) {
                    net.minecraft.entity.Entity entity=player.world.getEntityByID(m.entityId);
                    boolean accepted=entity instanceof com.wartec.wartecmod.port.entity.EntityCustomCruise
                        && ((com.wartec.wartecmod.port.entity.EntityCustomCruise)entity).getBuild().checksum()==build.checksum()
                        && ((com.wartec.wartecmod.port.entity.EntityCustomCruise)entity).applyCommand(mission,player);
                    CruiseText.tell(player,accepted?"program.updated":"program.update_rejected");return;
                }
                if(m.action==6) {
                    if(mission.getMode()!=CruiseMission.Mode.SEARCH || !Double.isFinite(m.x) || m.x!=Math.rint(m.x)
                            || m.x<0 || m.x>=CruiseTargetCategory.values().length
                            || !mission.setCategory(CruiseTargetCategory.values()[(int)m.x],build)) { CruiseText.tell(player,"program.category_unavailable");return; }
                }
                else if(m.action==5) {
                    if((m.x!=0 && m.x!=1) || !mission.setMode(m.x==1?CruiseMission.Mode.SEARCH:CruiseMission.Mode.COORDINATE,build)) { CruiseText.tell(player,"program.seeker_required");return; }
                }
                else if(m.action==1) mission.removeLast();
                else if(m.action==2) mission.clear();
                else {
                    Vec3d target=new Vec3d(m.x,m.y,m.z);
                    if(m.action==3) {
                        ItemStack designator=container.getDesignator();
                        Vec3d point=DesignatorCompat.getSavedTarget(player.world,designator,m.y);
                        if(point==null) {
                            net.minecraft.nbt.NBTTagCompound previous=mission.write();
                            boolean queued=DesignatorCompat.resolveSavedTarget(player.getServerWorld(),designator,m.y,
                                ()->!player.isDead && player.openContainer==container && container.canInteractWith(player)
                                    && container.getDesignator()==designator && player.inventory.getStackInSlot(m.slot)==stack
                                    && CruiseMission.fromStack(stack).write().equals(previous),resolved->{
                                    CruiseMission updated=CruiseMission.fromStack(stack);Vec3d goal=resolved.addVector(.5,0,.5);
                                    boolean ok=updated.getMode()==CruiseMission.Mode.COORDINATE?updated.setTarget(goal,player.dimension):updated.append(goal,player.dimension);
                                    if(ok && updated.isValidFor(build,player.dimension)) {
                                        updated.writeToStack(stack);player.inventory.markDirty();player.openContainer.detectAndSendChanges();
                                    } else CruiseText.tell(player,"program.rejected");
                                });
                            CruiseText.tell(player,queued?"program.resolving_y":"program.designator_incomplete");return;
                        }
                        target=point.addVector(0.5,0,0.5);
                    }
                    boolean added=mission.getMode()==CruiseMission.Mode.COORDINATE?mission.setTarget(target,player.dimension):mission.append(target,player.dimension);
                    if(!added || !mission.isValidFor(build,player.dimension)) {
                        CruiseText.tell(player,"program.rejected");return;
                    }
                }
                mission.writeToStack(stack);player.inventory.markDirty();player.openContainer.detectAndSendChanges();
            });
            return null;
        }
    }
}
