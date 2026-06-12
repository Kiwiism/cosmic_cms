package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import scripting.event.EventInstanceManager;

public class EventInfoCommand extends Command {
    {
        setDescription("Show whether you are attached to an event instance.");
    }

    @Override
    public void execute(Client client, String[] params) {
        // Read-only recovery insight for players who suspect stale event state.
        Character player = client.getPlayer();
        EventInstanceManager event = player.getEventInstance();
        if (event == null) {
            player.message("You are not attached to an event instance.");
            return;
        }
        player.message("You are attached to event instance: " + event.getName());
    }
}
