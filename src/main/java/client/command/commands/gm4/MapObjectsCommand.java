package client.command.commands.gm4;

import client.Client;
import client.command.Command;
import server.maps.MapObject;

public class MapObjectsCommand extends Command {
    {
        setDescription("List every object type and object ID on the current map.");
    }

    @Override
    public void execute(Client client, String[] params) {
        // Read-only environment diagnostic for moderators.
        client.getPlayer().yellowMessage("Objects on map " + client.getPlayer().getMapId() + ":");
        for (MapObject object : client.getPlayer().getMap().getMapObjects()) {
            client.getPlayer().message(object.getType() + " - OID " + object.getObjectId());
        }
    }
}
