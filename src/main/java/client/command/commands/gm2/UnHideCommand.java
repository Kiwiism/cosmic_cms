package client.command.commands.gm2;

import client.Character;
import client.Client;
import client.command.Command;

public class UnHideCommand extends Command {
    {
        setDescription("Become visible to players.");
    }

    @Override
    public void execute(Client client, String[] params) {
        Character player = client.getPlayer();
        if (!player.isHidden()) {
            player.yellowMessage("You are already visible.");
            return;
        }

        player.Hide(false);
        player.yellowMessage("You are now visible to players.");
    }
}
