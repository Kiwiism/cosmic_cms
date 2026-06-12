package client.command.commands.gm1;

import client.Character;
import client.Client;
import client.command.Command;

public class ResetApCommand extends Command {
    {
        setDescription("Reset primary stats to 4 and refund the assigned AP.");
    }

    @Override
    public void execute(Client client, String[] params) {
        Character player = client.getPlayer();
        int refundedAp = player.resetAbilityPoints();

        if (refundedAp < 0) {
            player.yellowMessage("AP reset cancelled because the refunded AP would exceed the configured AP limit.");
        } else if (refundedAp == 0) {
            player.yellowMessage("Your primary stats are already fully reset.");
        } else {
            player.yellowMessage("Primary stats reset. Refunded " + refundedAp + " AP.");
        }
    }
}
