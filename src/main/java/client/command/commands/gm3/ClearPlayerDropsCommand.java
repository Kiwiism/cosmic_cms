package client.command.commands.gm3;

import client.Character;
import client.Client;
import client.command.Command;

public class ClearPlayerDropsCommand extends Command {
    {
        setDescription("Remove drops owned by a specified online player.");
    }

    @Override
    public void execute(Client client, String[] params) {
        // Targeted support action; only removes drops attributed to the selected player.
        if (params.length < 1) {
            client.getPlayer().yellowMessage("Syntax: !clearplayerdrops <playername>");
            return;
        }
        Character target = client.getWorldServer().getPlayerStorage().getCharacterByName(params[0]);
        if (target == null) {
            client.getPlayer().message("Player '" + params[0] + "' was not found in this world.");
            return;
        }
        target.getMap().clearDrops(target);
        client.getPlayer().message("Cleared drops owned by " + target.getName() + ".");
    }
}
