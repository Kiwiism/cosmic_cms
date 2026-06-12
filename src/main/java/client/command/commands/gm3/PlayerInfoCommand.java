package client.command.commands.gm3;

import client.Character;
import client.Client;
import client.command.Command;

public class PlayerInfoCommand extends Command {
    {
        setDescription("Show support-safe information about an online player.");
    }

    @Override
    public void execute(Client client, String[] params) {
        // Staff support view deliberately excludes account, IP, and hardware identifiers.
        if (params.length < 1) {
            client.getPlayer().yellowMessage("Syntax: !playerinfo <playername>");
            return;
        }
        Character target = client.getWorldServer().getPlayerStorage().getCharacterByName(params[0]);
        if (target == null) {
            client.getPlayer().message("Player '" + params[0] + "' was not found in this world.");
            return;
        }
        client.getPlayer().yellowMessage(target.getName() + " - level " + target.getLevel() + " " + target.getJob());
        client.getPlayer().message("World/channel/map: " + target.getWorld() + "/"
                + target.getClient().getChannel() + "/" + target.getMapId());
        client.getPlayer().message("HP/MP: " + target.getHp() + "/" + target.getCurrentMaxHp()
                + " | " + target.getMp() + "/" + target.getCurrentMaxMp());
        client.getPlayer().message("GM level: " + target.gmLevel() + ", party: " + target.getPartyId()
                + ", event: " + (target.getEventInstance() == null ? "none" : target.getEventInstance().getName()));
    }
}
