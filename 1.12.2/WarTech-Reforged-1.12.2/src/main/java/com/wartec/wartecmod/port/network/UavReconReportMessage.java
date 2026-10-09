package com.wartec.wartecmod.port.network;

import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.uav.UavReconReport;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Sends a bounded reconnaissance report to the dedicated client viewer. */
public final class UavReconReportMessage implements IMessage {
    private String title = "UAV RECON REPORT";
    private UavReconReport report = new UavReconReport();

    public UavReconReportMessage() {
    }

    public UavReconReportMessage(String title, UavReconReport report) {
        this.title = title == null ? "UAV RECON REPORT" : title;
        this.report = report == null ? new UavReconReport() : report.copy();
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        title = ByteBufUtils.readUTF8String(buffer);
        NBTTagCompound tag = ByteBufUtils.readTag(buffer);
        report = UavReconReport.readFromNbt(tag);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        ByteBufUtils.writeUTF8String(buffer, title);
        ByteBufUtils.writeTag(buffer, report.writeToNbt());
    }

    public static final class Handler
            implements IMessageHandler<UavReconReportMessage, IMessage> {
        @Override
        public IMessage onMessage(UavReconReportMessage message,
                MessageContext context) {
            WarTechReforged.proxy.openUavReconReport(
                    message.title, message.report);
            return null;
        }
    }
}
