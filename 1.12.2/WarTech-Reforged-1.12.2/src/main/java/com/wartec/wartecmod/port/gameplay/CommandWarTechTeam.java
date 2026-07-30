package com.wartec.wartecmod.port.gameplay;

import com.wartec.wartecmod.port.integration.PlayerTeamPersistence;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public final class CommandWarTechTeam extends CommandBase {
    @Override
    public String getName() {
        return "wtteam";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/wtteam <status|join <name>|clear>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] arguments)
        throws CommandException {
        EntityPlayer player = getCommandSenderAsPlayer(sender);
        if (arguments.length == 0 || "status".equalsIgnoreCase(arguments[0])) {
            player.sendMessage(new TextComponentString(
                "WarTech IFF team: " + PlayerTeamPersistence.getPlayerTeam(player)
            ));
            return;
        }
        if ("clear".equalsIgnoreCase(arguments[0])) {
            PlayerTeamPersistence.setPlayerTeam(player, "");
            player.sendMessage(new TextComponentString("WarTech IFF team cleared"));
            return;
        }
        if ("join".equalsIgnoreCase(arguments[0]) && arguments.length >= 2) {
            String team = PlayerTeamPersistence.setPlayerTeam(player, arguments[1]);
            player.sendMessage(new TextComponentString("WarTech IFF team: " + team));
            return;
        }
        throw new CommandException(getUsage(sender));
    }
}
