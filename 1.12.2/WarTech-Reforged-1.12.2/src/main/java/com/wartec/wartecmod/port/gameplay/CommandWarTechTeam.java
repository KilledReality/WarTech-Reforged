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
        return "/wtteam <status|join <team> [player]|clear [player]>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] arguments)
        throws CommandException {
        if (arguments.length == 0 || "status".equalsIgnoreCase(arguments[0])) {
            EntityPlayer player = getCommandSenderAsPlayer(sender);
            player.sendMessage(new TextComponentString(
                "WarTech IFF team: " + PlayerTeamPersistence.getPlayerTeam(player)
            ));
            return;
        }
        // A public self-join command grants the joining player the other team's intel and roles.
        if (!sender.canUseCommand(2,getName())) throw new CommandException("commands.generic.permission");
        if ("clear".equalsIgnoreCase(arguments[0])) {
            EntityPlayer player=arguments.length>=2?getPlayer(server,sender,arguments[1]):getCommandSenderAsPlayer(sender);
            PlayerTeamPersistence.setPlayerTeam(player, "");
            player.sendMessage(new TextComponentString("WarTech IFF team cleared"));
            return;
        }
        if ("join".equalsIgnoreCase(arguments[0]) && arguments.length >= 2) {
            EntityPlayer player=arguments.length>=3?getPlayer(server,sender,arguments[2]):getCommandSenderAsPlayer(sender);
            String team = PlayerTeamPersistence.setPlayerTeam(player, arguments[1]);
            player.sendMessage(new TextComponentString("WarTech IFF team: " + team));
            return;
        }
        throw new CommandException(getUsage(sender));
    }
}
