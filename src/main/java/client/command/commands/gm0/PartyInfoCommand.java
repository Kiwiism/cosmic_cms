package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import net.server.world.PartyCharacter;

public class PartyInfoCommand extends Command {
    {
        setDescription("Show your party members and their online locations.");
    }

    @Override
    public void execute(Client client, String[] params) {
        // Party members already share this information through normal party UI.
        Character player = client.getPlayer();
        if (player.getParty() == null) {
            player.message("You are not in a party.");
            return;
        }

        player.yellowMessage("Party members:");
        for (PartyCharacter member : player.getParty().getMembers()) {
            String location = member.isOnline()
                    ? "channel " + member.getChannel() + ", map " + member.getMapId()
                    : "offline";
            player.message(member.getName() + " - " + location);
        }
    }
}
