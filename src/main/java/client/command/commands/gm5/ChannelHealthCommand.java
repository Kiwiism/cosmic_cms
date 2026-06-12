package client.command.commands.gm5;

import client.Client;
import client.command.Command;
import net.server.channel.Channel;

public class ChannelHealthCommand extends Command {
    {
        setDescription("Show population and merchant health for the current channel.");
    }

    @Override
    public void execute(Client client, String[] params) {
        // Read-only channel operations summary.
        Channel channel = client.getChannelServer();
        client.getPlayer().yellowMessage("Channel " + channel.getId() + " health");
        client.getPlayer().message("Players: " + channel.getPlayerStorage().getSize()
                + ", load display: " + channel.getChannelCapacity());
        client.getPlayer().message("Hired merchants: " + channel.getHiredMerchants().size()
                + ", active expeditions: " + channel.getExpeditions().size());
    }
}
