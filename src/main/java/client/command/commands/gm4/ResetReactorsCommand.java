package client.command.commands.gm4;

import client.Client;
import client.command.Command;

public class ResetReactorsCommand extends Command {
    {
        setDescription("Reset all reactors on the current map.");
    }

    @Override
    public void execute(Client client, String[] params) {
        // Environment-changing moderator action scoped to the current map.
        client.getPlayer().getMap().resetReactors();
        client.getPlayer().message("Reactors on the current map have been reset.");
    }
}
