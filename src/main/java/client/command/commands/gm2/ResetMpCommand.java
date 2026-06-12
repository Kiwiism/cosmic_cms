package client.command.commands.gm2;

import client.Character;
import client.Client;
import client.command.Command;

public class ResetMpCommand extends Command {
    {
        setDescription("Reset base MP to average level and job progression.");
    }

    @Override
    public void execute(Client client, String[] params) {
        Character player = client.getPlayer();
        int[] target = player.resetAverageHpMp(false, true);
        player.yellowMessage("Base MP reset to " + target[1] + ".");
    }
}
