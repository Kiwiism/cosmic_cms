package client.command.commands.gm5;

import client.Client;
import client.command.Command;
import net.server.channel.Channel;
import net.server.world.World;

public class WorldHealthCommand extends Command {
    {
        setDescription("Show world population, channels, and active rates.");
    }

    @Override
    public void execute(Client client, String[] params) {
        // Read-only world operations summary.
        World world = client.getWorldServer();
        client.getPlayer().yellowMessage("World " + world.getId() + " health");
        client.getPlayer().message("Channels: " + world.getChannelsSize()
                + ", online: " + world.getPlayerStorage().getSize());
        client.getPlayer().message("Rates EXP/Meso/Drop/Boss/Quest: "
                + world.getExpRate() + "/" + world.getMesoRate() + "/"
                + world.getDropRate() + "/" + world.getBossDropRate() + "/"
                + world.getQuestRate());
        for (Channel channel : world.getChannels()) {
            client.getPlayer().message("Channel " + channel.getId() + ": "
                    + channel.getPlayerStorage().getSize() + " players.");
        }
    }
}
