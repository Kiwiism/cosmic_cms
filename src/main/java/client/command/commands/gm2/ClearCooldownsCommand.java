package client.command.commands.gm2;

import client.Client;
import client.command.Command;

public class ClearCooldownsCommand extends Command {
    {
        setDescription("Clear all skill cooldowns from your own character.");
    }

    @Override
    public void execute(Client client, String[] params) {
        // Self-only tester reset. Skill ID -1 means no cooldown is preserved.
        client.getPlayer().removeAllCooldownsExcept(-1, true);
        client.getPlayer().message("Your cooldowns have been cleared.");
    }
}
