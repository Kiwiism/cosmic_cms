package client.command.commands.gm2;

import client.Character;
import client.Client;
import client.command.Command;

public class ResetHpCommand extends Command {
    {
        setDescription("Reset base HP to average level and job progression.");
    }

    @Override
    public void execute(Client client, String[] params) {
        Character player = client.getPlayer();
        int[] target = player.resetAverageHpMp(true, false);
        player.yellowMessage("Base HP reset to " + target[0] + ".");
    }
}
