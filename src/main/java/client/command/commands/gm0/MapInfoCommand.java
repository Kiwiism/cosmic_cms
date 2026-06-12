package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import server.maps.MapleMap;

public class MapInfoCommand extends Command {
    {
        setDescription("Show basic information about your current map.");
    }

    @Override
    public void execute(Client client, String[] params) {
        // Read-only player insight: no object IDs or restricted diagnostic state.
        Character player = client.getPlayer();
        MapleMap map = player.getMap();
        player.yellowMessage(map.getStreetName() + " - " + map.getMapName());
        player.message("Map ID: " + map.getId() + ", channel: " + client.getChannel());
    }
}
