package com.wartec.wartecmod.tileentity.vls;

import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.BufPacket;
import com.hbm.tileentity.IBufPacketReceiver;
import com.hbm.tileentity.TileEntityLoadedBase;
import cpw.mods.fml.common.network.NetworkRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

public class TileEntityVlsExhaust extends TileEntityLoadedBase implements IBufPacketReceiver {
    public int openingAnimation = 0;
    public boolean open = false;

    public void func_145839_a(NBTTagCompound nbt) {
        super.func_145839_a(nbt);
        this.openingAnimation = nbt.func_74762_e("openanim");
        this.open = nbt.func_74767_n("open");
    }

    public void func_145841_b(NBTTagCompound nbt) {
        super.func_145841_b(nbt);
        nbt.func_74768_a("openanim", this.openingAnimation);
        nbt.func_74757_a("open", this.open);
    }

    public void func_145845_h() {
        if (!this.field_145850_b.field_72995_K) {
            if (this.open && this.openingAnimation < 90) {
                this.openingAnimation += 3;
            }

            if (!this.open && this.openingAnimation > 0) {
                this.openingAnimation -= 3;
            }

            PacketDispatcher.wrapper.sendToAllAround(
                    new BufPacket(this.field_145851_c, this.field_145848_d, this.field_145849_e, this),
                    new NetworkRegistry.TargetPoint(
                            this.field_145850_b.field_73011_w.field_76574_g,
                            this.field_145851_c,
                            this.field_145848_d,
                            this.field_145849_e,
                            50.0D));
        }
    }

    public void serialize(ByteBuf buf) {
        buf.writeInt(this.openingAnimation);
    }

    public void deserialize(ByteBuf buf) {
        this.openingAnimation = buf.readInt();
    }

    public AxisAlignedBB getRenderBoundingBox() {
        return TileEntity.INFINITE_EXTENT_AABB;
    }

    public double func_145833_n() {
        return 65536.0D;
    }
}
