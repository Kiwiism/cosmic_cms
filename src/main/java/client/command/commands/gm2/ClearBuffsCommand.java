package client.command.commands.gm2;

import client.Client;
import client.command.Command;

public class ClearBuffsCommand extends Command {
    {
        setDescription("Clear all buffs from your own character.");
    }

    @Override
    public void execute(Client client, String[] params) {
        // Self-only tester reset for reproducing buff interactions.
        client.getPlayer().cancelAllBuffs(false);
        client.getPlayer().message("Your buffs have been cleared.");
    }
}
