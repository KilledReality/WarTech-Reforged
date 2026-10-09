package com.wartec.wartecmod.port.gameplay;

import com.wartec.wartecmod.port.integration.MissileChunkLoader;
import com.wartec.wartecmod.port.integration.FlightTickMetrics;
import java.util.Locale;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;

/** Operator diagnostics only; never spawns weapons or changes flight limits. */
public final class CommandWarTechFlight extends CommandBase {
    @Override public String getName() { return "wtflight"; }
    @Override public String getUsage(ICommandSender sender) { return "/wtflight [status|reset]"; }
    @Override public int getRequiredPermissionLevel() { return 2; }
    @Override public void execute(MinecraftServer server,ICommandSender sender,String[] args) throws CommandException {
        if(args.length>1 || args.length==1 && !"status".equalsIgnoreCase(args[0]) && !"reset".equalsIgnoreCase(args[0]))
            throw new CommandException(getUsage(sender));
        if(args.length==1 && "reset".equalsIgnoreCase(args[0])) {
            MissileChunkLoader.resetDiagnostics();sender.sendMessage(new TextComponentTranslation("flight.diagnostics.reset"));return;
        }
        FlightTickMetrics samples=MissileChunkLoader.tickMetrics();
        double average=samples.meanMillis(),p95=samples.p95Millis();
        sender.sendMessage(new TextComponentTranslation("flight.diagnostics.header",
            String.format(Locale.ROOT,"%.2f",average),String.format(Locale.ROOT,"%.2f",p95),
            String.format(Locale.ROOT,"%.1f",average<=0?0:Math.min(20,1000/average))));
        for(WorldServer world:server.worlds) if(world!=null)
            sender.sendMessage(new TextComponentString("WarTech " + MissileChunkLoader.diagnostics(world)));
    }
}
