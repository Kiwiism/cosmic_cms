package client.command.commands.gm2;

import client.Character;
import client.Client;
import client.command.Command;

public class ResetHpMpCommand extends Command {
    {
        setDescription("Reset base HP and MP to average level and job progression.");
    }

    @Override
    public void execute(Client client, String[] params) {
        Character player = client.getPlayer();
        int[] target = player.resetAverageHpMp(true, true);
        player.yellowMessage("Base HP/MP reset to " + target[0] + "/" + target[1] + ".");
    }
}
