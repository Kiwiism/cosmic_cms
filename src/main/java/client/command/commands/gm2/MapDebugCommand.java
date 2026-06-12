package client.command.commands.gm2;

import client.Client;
import client.command.Command;
import server.maps.MapleMap;

public class MapDebugCommand extends Command {
    {
        setDescription("Show object counts for the current map.");
    }

    @Override
    public void execute(Client client, String[] params) {
        // Read-only map diagnostic intended for sandbox/tester characters.
        MapleMap map = client.getPlayer().getMap();
        client.getPlayer().yellowMessage(map.getMapName() + " (" + map.getId() + ")");
        client.getPlayer().message("Players: " + map.countPlayers() + ", monsters: "
                + map.countMonsters() + ", bosses: " + map.countBosses());
        client.getPlayer().message("Reactors: " + map.countReactors() + ", drops: "
                + map.getDroppedItemCount() + ", total objects: " + map.getMapObjects().size());
    }
}
