package client.command.commands.gm0;

import client.Client;
import client.command.Command;
import net.server.PlayerCoolDownValueHolder;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class CooldownsCommand extends Command {
    {
        setDescription("List your active skill cooldowns.");
    }

    @Override
    public void execute(Client client, String[] params) {
        // Read-only snapshot using the character's persisted cooldown data.
        List<PlayerCoolDownValueHolder> cooldowns = client.getPlayer().getAllCooldowns();
        if (cooldowns.isEmpty()) {
            client.getPlayer().message("You have no active cooldowns.");
            return;
        }

        long now = System.currentTimeMillis();
        client.getPlayer().yellowMessage("Active cooldowns: " + cooldowns.size());
        for (PlayerCoolDownValueHolder cooldown : cooldowns) {
            long remaining = Math.max(0, cooldown.startTime + cooldown.length - now);
            client.getPlayer().message("Skill " + cooldown.skillId + ": "
                    + TimeUnit.MILLISECONDS.toSeconds(remaining) + "s remaining.");
        }
    }
}
